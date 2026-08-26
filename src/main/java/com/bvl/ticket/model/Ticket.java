package com.bvl.ticket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

/**
 * Repräsentiert ein Ticket-Objekt im System.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
/**
 * @author Omar
 */
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "ticket_number", unique = true, nullable = false)
    private int ticketNumber;

    @Column(name = "function_name", length = 255)
    private String functionName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", length = 20, nullable = false)
    private FeatureType featureType = FeatureType.TASK;

    @Column(name = "affected_database", length = 100)
    private String affectedDatabase = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private TicketStatus status = TicketStatus.OFFEN;

    @Column(name = "is_public")
    private boolean isPublic = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ticket_screenshots", joinColumns = @JoinColumn(name = "ticket_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", columnDefinition = "text")
    private List<String> screenshotUrls = new ArrayList<>();

    // Admin-spezifische Felder
    @Column(name = "checked_in_scm")
    private boolean checkedInScm = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "tested_by_developers")
    private String testedByDevelopers;

    @Column(name = "time_effort")
    private Double timeEffort;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_unit", length = 10)
    private TimeUnit timeUnit = TimeUnit.HOURS;

    @Column(name = "admin_comment", columnDefinition = "text")
    private String adminComment;

    // Informationen zur Erstellung
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_by_name", length = 255)
    private String createdByName;

    @Column(name = "created_by_role", length = 50)
    private String createdByRole;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "assigned_to_name", length = 255)
    private String assignedToName;

    // Zeitstempel
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "resolved_at")
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
