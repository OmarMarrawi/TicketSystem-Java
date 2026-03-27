package com.bvl.ticket.service;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.Ticket;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
/**
 * @author Omar
 */
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    // Datenbank-Feldnamen als Konstanten zur Vermeidung von Fehlern
    private static final String F_ID = "id";
    private static final String F_TICKET_NUMBER = "ticket_number";
    private static final String F_FUNCTION_NAME = "function_name";
    private static final String F_DESCRIPTION = "description";
    private static final String F_FEATURE_TYPE = "feature_type";
    private static final String F_AFFECTED_DB = "affected_database";
    private static final String F_PRIORITY = "priority";
    private static final String F_STATUS = "status";
    private static final String F_IS_PUBLIC = "is_public";
    private static final String F_SCM = "checked_in_scm";
    private static final String F_SCM_AT = "checked_in_at";
    private static final String F_TESTED = "tested_by_developers";
    private static final String F_EFFORT = "time_effort";
    private static final String F_UNIT = "time_unit";
    private static final String F_ADMIN_COMMENT = "admin_comment";
    private static final String F_CREATED_BY = "created_by";
    private static final String F_CREATED_BY_NAME = "created_by_name";
    private static final String F_CREATED_BY_ROLE = "created_by_role";
    private static final String F_ASSIGNED_TO = "assigned_to_name";
    private static final String F_CREATED_AT = "created_at";
    private static final String F_UPDATED_AT = "updated_at";
    private static final String F_SCREENSHOT_URLS = "screenshot_urls";

    @Inject
    private MongoService mongoService;

    /**
     * Holt alle Tickets aus der Datenbank, sortiert nach Nummer.
     */
    public List<Ticket> getAllTickets() {
        List<Ticket> tickets = new ArrayList<>();
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            for (Document doc : collection.find().sort(Sorts.descending(F_TICKET_NUMBER))) {
                tickets.add(mapDocumentToTicket(doc));
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden aller Tickets", e);
        }
        return tickets;
    }

    /**
     * Holt nur die Tickets eines bestimmten Referats.
     */
    public List<Ticket> getTicketsByRole(String roleName) {
        List<Ticket> tickets = new ArrayList<>();
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            for (Document doc : collection.find(Filters.eq(F_CREATED_BY_ROLE, roleName))
                    .sort(Sorts.descending(F_TICKET_NUMBER))) {
                tickets.add(mapDocumentToTicket(doc));
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Referats-Tickets für Role: {}", roleName, e);
        }
        return tickets;
    }

    /**
     * Berechnet die Statistiken für das Dashboard.
     */
    public com.bvl.ticket.model.DashboardStats getDashboardStats(User user) {
        try {
            List<Ticket> tickets = (user.getRole() == com.bvl.ticket.model.UserRole.ADMIN)
                    ? getAllTickets()
                    : getTicketsByRole(user.getRole().getLabel());

            Map<String, Long> byStatus = new HashMap<>();
            for (Ticket.TicketStatus s : Ticket.TicketStatus.values()) {
                byStatus.put(s.getLabel(), 0L);
            }
            tickets.stream()
                    .filter(t -> t.getStatus() != null)
                    .forEach(t -> byStatus.merge(t.getStatus().getLabel(), 1L, Long::sum));

            Map<String, Long> byPriority = new HashMap<>();
            for (Ticket.TicketPriority p : Ticket.TicketPriority.values()) {
                byPriority.put(p.getLabel(), 0L);
            }
            tickets.stream()
                    .filter(t -> t.getPriority() != null)
                    .forEach(t -> byPriority.merge(t.getPriority().getLabel(), 1L, Long::sum));

            Map<String, Long> byRole = tickets.stream()
                    .filter(t -> t.getCreatedByRole() != null)
                    .collect(Collectors.groupingBy(Ticket::getCreatedByRole, Collectors.counting()));

            List<Ticket> recent = tickets.stream()
                    .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                    .limit(5)
                    .collect(Collectors.toList());

            return com.bvl.ticket.model.DashboardStats.builder()
                    .totalTickets(tickets.size())
                    .byStatus(byStatus)
                    .byPriority(byPriority)
                    .byRole(byRole)
                    .recentTickets(recent)
                    .build();
        } catch (Exception e) {
            logger.error("Fehler bei der Dashboard-Statistik-Generierung", e);
            return com.bvl.ticket.model.DashboardStats.builder()
                    .totalTickets(0).byStatus(new HashMap<>()).byPriority(new HashMap<>()).byRole(new HashMap<>())
                    .recentTickets(new ArrayList<>()).build();
        }
    }

    /**
     * Speichert ein neues Ticket in MongoDB.
     */
    public void saveTicket(Ticket t) {
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            t.setTicketNumber(getNextTicketNumber());
            collection.insertOne(mapTicketToDocument(t));
            logger.info("Ticket #{} erfolgreich gespeichert", t.getTicketNumber());
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des Tickets", e);
        }
    }

    /**
     * Holt ein einzelnes Ticket anhand seiner ID.
     */
    public Ticket getTicketById(String id) {
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            Document doc = collection.find(Filters.eq(F_ID, id)).first();
            return doc != null ? mapDocumentToTicket(doc) : null;
        } catch (Exception e) {
            logger.error("Fehler beim Laden von Ticket ID: {}", id, e);
            return null;
        }
    }

    /**
     * Aktualisiert ein bestehendes Ticket.
     */
    public void updateTicket(Ticket t) {
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            t.setUpdatedAt(LocalDateTime.now());
            collection.replaceOne(Filters.eq(F_ID, t.getId()), mapTicketToDocument(t));
            logger.info("Ticket #{} erfolgreich aktualisiert", t.getTicketNumber());
        } catch (Exception e) {
            logger.error("Fehler beim Update von Ticket #{}", t.getTicketNumber(), e);
        }
    }

    /**
     * Löscht ein Ticket aus der Datenbank.
     */
    public void deleteTicket(String id) {
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            collection.deleteOne(Filters.eq(F_ID, id));
            logger.info("Ticket mit ID {} erfolgreich gelöscht", id);
        } catch (Exception e) {
            logger.error("Fehler beim Löschen des Tickets mit ID: {}", id, e);
        }
    }

    /**
     * Ermittelt die höchste Ticketnummer und zählt sie um 1 hoch.
     */
    private int getNextTicketNumber() {
        try {
            MongoCollection<Document> collection = mongoService.getTicketsCollection();
            Document lastTicket = collection.find().sort(Sorts.descending(F_TICKET_NUMBER)).first();
            return (lastTicket != null) ? lastTicket.getInteger(F_TICKET_NUMBER, 0) + 1 : 1;
        } catch (Exception e) {
            logger.error("Fehler bei der Nummern-Generierung, verwende Standard 1", e);
            return 1;
        }
    }

    /**
     * Mapping: Java-Objekt -> MongoDB Dokument
     */
    private Document mapTicketToDocument(Ticket t) {
        Document doc = new Document();
        doc.append(F_ID, t.getId());
        doc.append(F_TICKET_NUMBER, t.getTicketNumber());
        doc.append(F_FUNCTION_NAME, t.getFunctionName());
        doc.append(F_DESCRIPTION, t.getDescription());
        doc.append(F_FEATURE_TYPE, t.getFeatureType().name());
        doc.append(F_AFFECTED_DB, t.getAffectedDatabase());
        doc.append(F_PRIORITY,
                t.getPriority().name().substring(0, 1) + t.getPriority().name().substring(1).toLowerCase());
        doc.append(F_STATUS, t.getStatus().getLabel());
        doc.append(F_IS_PUBLIC, t.isPublic());
        doc.append(F_SCM, t.isCheckedInScm());
        if (t.getCheckedInAt() != null) {
            doc.append(F_SCM_AT, t.getCheckedInAt().toString());
        }
        doc.append(F_TESTED, t.getTestedByDevelopers());
        doc.append(F_EFFORT, t.getTimeEffort());
        doc.append(F_UNIT, t.getTimeUnit().name());
        doc.append(F_ADMIN_COMMENT, t.getAdminComment());
        doc.append(F_CREATED_BY, t.getCreatedBy());
        doc.append(F_CREATED_BY_NAME, t.getCreatedByName());
        doc.append(F_CREATED_BY_ROLE, t.getCreatedByRole());
        doc.append(F_ASSIGNED_TO, t.getAssignedToName());
        doc.append(F_CREATED_AT, t.getCreatedAt().toString());
        doc.append(F_UPDATED_AT, t.getUpdatedAt().toString());
        doc.append(F_SCREENSHOT_URLS, t.getScreenshotUrls());
        return doc;
    }

    /**
     * Mapping: MongoDB Dokument -> Java-Objekt
     */
    private Ticket mapDocumentToTicket(Document doc) {
        Ticket t = new Ticket();
        t.setId(doc.getString(F_ID));
        t.setTicketNumber(doc.getInteger(F_TICKET_NUMBER, 0));
        t.setFunctionName(doc.getString(F_FUNCTION_NAME));
        t.setDescription(doc.getString(F_DESCRIPTION));
        t.setFeatureType(Ticket.FeatureType.valueOf(doc.getString(F_FEATURE_TYPE)));
        t.setAffectedDatabase(doc.getString(F_AFFECTED_DB));
        t.setPriority(Ticket.TicketPriority.valueOf(doc.getString(F_PRIORITY).toUpperCase()));

        String statusLabel = doc.getString(F_STATUS);
        for (Ticket.TicketStatus status : Ticket.TicketStatus.values()) {
            if (status.getLabel().equalsIgnoreCase(statusLabel)) {
                t.setStatus(status);
                break;
            }
        }

        t.setPublic(doc.getBoolean(F_IS_PUBLIC, true));
        t.setCheckedInScm(doc.getBoolean(F_SCM, false));
        if (doc.containsKey(F_SCM_AT)) {
            t.setCheckedInAt(toLocalDateTime(doc.get(F_SCM_AT)));
        }
        t.setTestedByDevelopers(doc.getString(F_TESTED));
        t.setTimeEffort(doc.getDouble(F_EFFORT));
        if (doc.containsKey(F_UNIT)) {
            t.setTimeUnit(Ticket.TimeUnit.valueOf(doc.getString(F_UNIT)));
        }
        t.setAdminComment(doc.getString(F_ADMIN_COMMENT));
        t.setCreatedBy(doc.getString(F_CREATED_BY));
        t.setCreatedByName(doc.getString(F_CREATED_BY_NAME));
        t.setCreatedByRole(doc.getString(F_CREATED_BY_ROLE));
        t.setAssignedToName(doc.getString(F_ASSIGNED_TO));
        if (doc.containsKey(F_SCREENSHOT_URLS)) {
            t.setScreenshotUrls(doc.getList(F_SCREENSHOT_URLS, String.class));
        }

        t.setCreatedAt(toLocalDateTime(doc.get(F_CREATED_AT)));
        t.setUpdatedAt(toLocalDateTime(doc.get(F_UPDATED_AT)));

        return t;
    }

    private LocalDateTime toLocalDateTime(Object dateObj) {
        if (dateObj == null)
            return null;
        if (dateObj instanceof String) {
            return LocalDateTime.parse((String) dateObj);
        } else if (dateObj instanceof Date) {
            return ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}
