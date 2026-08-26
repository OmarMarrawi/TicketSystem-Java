package com.bvl.ticket.service;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.Ticket;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
/**
 * @author Omar
 */
public class TicketService {

    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);

    @Inject
    private DatabaseService database;

    /**
     * Holt alle Tickets aus der Datenbank, sortiert nach Nummer.
     */
    public List<Ticket> getAllTickets() {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT t FROM Ticket t ORDER BY t.ticketNumber DESC", Ticket.class)
                    .getResultList());
        } catch (Exception e) {
            logger.error("Fehler beim Laden aller Tickets", e);
            return new ArrayList<>();
        }
    }

    /**
     * Holt nur die Tickets eines bestimmten Referats.
     */
    public List<Ticket> getTicketsByRole(String roleName) {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT t FROM Ticket t WHERE t.createdByRole = :role ORDER BY t.ticketNumber DESC", Ticket.class)
                    .setParameter("role", roleName)
                    .getResultList());
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Referats-Tickets für Role: {}", roleName, e);
            return new ArrayList<>();
        }
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
     * Speichert ein neues Ticket in PostgreSQL.
     */
    public void saveTicket(final Ticket t) {
        try {
            database.run(em -> {
                t.setTicketNumber(getNextTicketNumber(em));
                em.persist(t);
            });
            logger.info("Ticket #{} erfolgreich gespeichert", t.getTicketNumber());
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des Tickets", e);
        }
    }

    /**
     * Holt ein einzelnes Ticket anhand seiner ID.
     * Die Screenshots werden direkt mitgeladen (JOIN FETCH), damit sie
     * auch nach Schließen der Session verfügbar sind.
     */
    public Ticket getTicketById(Long id) {
        try {
            List<Ticket> result = database.read(em -> em.createQuery(
                    "SELECT DISTINCT t FROM Ticket t LEFT JOIN FETCH t.screenshotUrls WHERE t.id = :id", Ticket.class)
                    .setParameter("id", id)
                    .getResultList());
            return result.isEmpty() ? null : result.get(0);
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
            t.setUpdatedAt(LocalDateTime.now());
            database.run(em -> em.merge(t));
            logger.info("Ticket #{} erfolgreich aktualisiert", t.getTicketNumber());
        } catch (Exception e) {
            logger.error("Fehler beim Update von Ticket #{}", t.getTicketNumber(), e);
        }
    }

    /**
     * Löscht ein Ticket aus der Datenbank.
     */
    public void deleteTicket(Long id) {
        try {
            database.run(em -> {
                Ticket ticket = em.find(Ticket.class, id);
                if (ticket != null) {
                    em.remove(ticket);
                }
            });
            logger.info("Ticket mit ID {} erfolgreich gelöscht", id);
        } catch (Exception e) {
            logger.error("Fehler beim Löschen des Tickets mit ID: {}", id, e);
        }
    }

    /**
     * Ermittelt die höchste Ticketnummer und zählt sie um 1 hoch.
     * Wird innerhalb derselben Transaktion wie das Speichern aufgerufen.
     */
    private int getNextTicketNumber(EntityManager em) {
        try {
            Integer max = em.createQuery(
                    "SELECT MAX(t.ticketNumber) FROM Ticket t", Integer.class)
                    .getSingleResult();
            return (max != null) ? max + 1 : 1;
        } catch (NoResultException e) {
            return 1;
        }
    }
}
