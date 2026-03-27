package com.bvl.ticket.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Hilfsklasse für die Anzeige der Statistiken auf dem Dashboard.
 */
@Data
@Builder
/**
 * @author Omar
 */
public class DashboardStats {
    private long totalTickets; // Gesamtzahl der Tickets
    private Map<String, Long> byStatus; // Verteilung nach Status (Offen, Gelöst, etc.)
    private Map<String, Long> byPriority; // Verteilung nach Priorität
    private Map<String, Long> byRole; // Verteilung nach Referat
    private List<Ticket> recentTickets; // Die Liste der neuesten Tickets
}



