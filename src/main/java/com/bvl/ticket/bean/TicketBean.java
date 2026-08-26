package com.bvl.ticket.bean;

import com.bvl.ticket.model.Ticket;
import com.bvl.ticket.model.User;
import com.bvl.ticket.model.UserRole;
import com.bvl.ticket.service.AuthService;
import com.bvl.ticket.service.TicketService;
import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import java.io.Serializable;
import java.util.List;

import com.bvl.ticket.model.DashboardStats;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Named
@ViewScoped
/**
 * @author Omar
 */
public class TicketBean implements Serializable {

    @Getter
    private List<Ticket> tickets;

    @Getter
    @lombok.Setter
    private List<Ticket> filteredTickets;

    @Getter
    private DashboardStats stats;

    @Getter
    @lombok.Setter
    private String statusFilter;

    @Getter
    @lombok.Setter
    private String priorityFilter;

    @Getter
    @lombok.Setter
    private Long userFilter;

    @Getter
    @lombok.Setter
    private String globalFilter;

    @Getter
    private List<User> allUsers;

    @Inject
    private TicketService ticketService;

    @Inject
    private AuthService authService;

    @Inject
    private LoginBean loginBean;

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * Lädt die Ticket-Daten und Statistiken neu.
     * Berücksichtigt dabei die Rolle des angemeldeten Benutzers.
     */
    public void refresh() {
        User user = loginBean.getCurrentUser();
        if (user != null) {
            // Statistiken laden
            stats = ticketService.getDashboardStats(user);

            // Tickets laden: Admins sehen alles, Referate nur ihre eigenen
            if (user.getRole() == UserRole.ADMIN) {
                tickets = ticketService.getAllTickets();
                // Alle Benutzer für den Ersteller-Filter (nur Admin)
                allUsers = authService.getAllUsers();
            } else {
                tickets = ticketService.getTicketsByRole(user.getRole().getLabel());
            }
        }
    }

    public Ticket.TicketStatus[] getStatuses() {
        return Ticket.TicketStatus.values();
    }

    public Ticket.TicketPriority[] getPriorities() {
        return Ticket.TicketPriority.values();
    }

    /**
     * Wird aufgerufen, wenn sich die Filter ändern.
     * Filtert die Tickets basierend auf Status und Priorität.
     */
    public void onFilterChange() {
        if (tickets == null) {
            filteredTickets = new java.util.ArrayList<>();
            return;
        }

        // Starte mit allen Tickets
        java.util.List<Ticket> result = new java.util.ArrayList<>(tickets);

        // Filter nach Status
        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            result = result.stream()
                    .filter(t -> t.getStatus() != null && statusFilter.equals(t.getStatus().getLabel()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Filter nach Priorität
        if (priorityFilter != null && !priorityFilter.trim().isEmpty()) {
            result = result.stream()
                    .filter(t -> t.getPriority() != null && priorityFilter.equals(t.getPriority().getLabel()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Filter nach Ersteller (nur relevant für Admins)
        if (userFilter != null) {
            result = result.stream()
                    .filter(t -> userFilter.equals(t.getCreatedBy()))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Globaler Filter (Suchfeld)
        if (globalFilter != null && !globalFilter.trim().isEmpty()) {
            String lowerSearch = globalFilter.toLowerCase().trim();
            result = result.stream()
                    .filter(t -> (t.getFunctionName() != null
                            && t.getFunctionName().toLowerCase().contains(lowerSearch)) ||
                            (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerSearch)) ||
                            (String.valueOf(t.getTicketNumber()).contains(lowerSearch)))
                    .collect(java.util.stream.Collectors.toList());
        }

        // Falls Filter gesetzt sind, setzen wir filteredTickets, sonst lassen wir es
        // null
        // (PrimeFaces zeigt dann die volle Liste 'tickets')
        if ((statusFilter == null || statusFilter.isEmpty()) &&
                (priorityFilter == null || priorityFilter.isEmpty()) &&
                (globalFilter == null || globalFilter.isEmpty()) &&
                userFilter == null) {
            filteredTickets = null;
        } else {
            filteredTickets = result;
        }
    }

    public void postProcessXLS(Object document) {
        XSSFWorkbook wb = (XSSFWorkbook) document;
        Sheet sheet = wb.getSheetAt(0);
        
        // Prüfe ob Header-Row existiert, sonst erstelle sie
        Row header = sheet.getRow(0);
        boolean headerCreated = false;
        
        // Definiere die erwarteten Header-Namen
        String[] headers = {
            "Ticket Nr", "Funktion", "Typ", "Betroffene DB", 
            "Status", "Prio", "Beschreibung", "Öffentlich"
        };

        if (header == null) {
            // Keine Zeile da -> erstelle Header
            sheet.shiftRows(0, sheet.getLastRowNum(), 1);
            header = sheet.createRow(0);
            headerCreated = true;
        } else {
            // Zeile da -> Prüfe ob es Daten sind (z.B. erste Zelle ist numerisch oder passt nicht zum Header)
            Cell firstCell = header.getCell(0);
            boolean isData = false;
            if (firstCell != null) {
                // Wenn erste Zelle eine Nummer ist oder nicht "Ticket Nr" heißt, ist es wohl Daten
                if (firstCell.getCellType() == CellType.NUMERIC || 
                   (firstCell.getCellType() == CellType.STRING && !firstCell.getStringCellValue().contains("Ticket"))) {
                    isData = true;
                }
            }
            
            if (isData) {
                // Verschiebe alles nach unten und erstelle neue Header-Zeile
                sheet.shiftRows(0, sheet.getLastRowNum(), 1);
                header = sheet.createRow(0);
                headerCreated = true;
            }
        }
        
        // Style für Fettgedruckt
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        
        // Wenn Header neu erstellt wurde oder wir sicherstellen wollen, dass die Texte stimmen:
        // Wir überschreiben die Header-Texte, um sicherzugehen, dass sie da sind.
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.getCell(i);
            if (cell == null) {
                cell = header.createCell(i);
            }
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        
        // Spaltenbreite anpassen (optional, aber nett)
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
