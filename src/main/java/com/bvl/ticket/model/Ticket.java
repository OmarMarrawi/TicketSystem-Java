package com.bvl.ticket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Repräsentiert ein Ticket-Objekt im System.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * @author Omar
 */
public class Ticket {
    private String id = UUID.randomUUID().toString();
    private int ticketNumber;
    private String functionName;
    private String description;
    private FeatureType featureType = FeatureType.TASK;
    private String affectedDatabase = "MongoDB";
    private TicketPriority priority;
    private TicketStatus status = TicketStatus.OFFEN;
    private boolean isPublic = true;
    private List<String> screenshotUrls = new ArrayList<>();

    // Admin-spezifische Felder
    private boolean checkedInScm = false;
    private LocalDateTime checkedInAt;
    private String testedByDevelopers;
    private Double timeEffort;
    private TimeUnit timeUnit = TimeUnit.HOURS;
    private String adminComment;

    // Informationen zur Erstellung
    private String createdBy;
    private String createdByName;
    private String createdByRole;
    private String assignedTo;
    private String assignedToName;

    // Zeitstempel
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;

    /**
     * Art des Tickets (Fehler, neue Funktion, etc.)
     */
    public enum FeatureType {
        BUG, FEATURE, IMPROVEMENT, TASK
    }

    /**
     * Dringlichkeit
     */
    public enum TicketPriority {
        HOCH("Hoch"),
        MITTEL("Mittel"),
        NIEDRIG("Niedrig");

        private String label;

        TicketPriority(String l) {
            this.label = l;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Aktueller Bearbeitungszustand
     */
    public enum TicketStatus {
        OFFEN("Offen"),
        IN_BEARBEITUNG("In Bearbeitung"),
        GELOEST("Gelöst"),
        GELOEST_BESTAETIGT("Gelöst bestätigt");

        private String label;

        TicketStatus(String l) {
            this.label = l;
        }

        public String getLabel() {
            return label;
        }
    }

    /**
     * Maßeinheit für den Zeitaufwand
     */
    public enum TimeUnit {
        HOURS("Stunden"), DAYS("Tage");

        private String label;

        TimeUnit(String l) {
            this.label = l;
        }

        public String getLabel() {
            return label;
        }
    }
}
