package com.bvl.ticket.bean;

import com.bvl.ticket.model.Ticket;
import com.bvl.ticket.service.TicketService;
import com.bvl.ticket.service.AuthService;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Base64;
import java.util.Map;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.event.FileUploadEvent;

@Named
@ViewScoped
/**
 * @author Omar
 */
public class TicketDetailBean implements Serializable {

    @Getter
    @Setter
    private Ticket ticket;

    @Getter
    @Setter
    private UploadedFile uploadedFile;

    @Inject
    private TicketService ticketService;

    @Inject
    private LoginBean loginBean;

    @Inject
    private com.bvl.ticket.service.AuthService authService; // Inject AuthService

    @Getter
    private java.util.List<com.bvl.ticket.model.User> assignableUsers; // Liste der zuweisbaren Benutzer

    @Getter
    @Setter
    private String assignedToUserId; // ID des ausgewählten Benutzers

    /**
     * Initialisiert die Detailansicht.
     * Liest die Ticket-ID aus dem URL-Parameter 'id'.
     */
    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String id = params.get("id");
        if (id != null) {
            ticket = ticketService.getTicketById(id);
            if (ticket != null && ticket.getAssignedTo() != null) {
                this.assignedToUserId = ticket.getAssignedTo();
            }
        }
        assignableUsers = authService.getAllUsers(); // Alle Benutzer laden
    }

    /**
     * Verarbeitet den Datei-Upload für ein bestehendes Ticket.
     * Das Bild wird in Base64 konvertiert und in die screenshotUrls-Liste
     * aufgenommen.
     * Ein Speichern des Tickets erfolgt erst beim Klick auf "Änderungen speichern"
     * (update).
     */
    /**
     * Listener für den automatischen Datei-Upload (Detailseite).
     */
    public void onFileUpload(FileUploadEvent event) {
        UploadedFile file = event.getFile();
        if (file != null && file.getContent() != null && file.getSize() > 0) {
            try {
                String encodedString = Base64.getEncoder().encodeToString(file.getContent());
                String contentType = file.getContentType();
                String dataUrl = "data:" + contentType + ";base64," + encodedString;

                if (ticket.getScreenshotUrls() == null) {
                    ticket.setScreenshotUrls(new java.util.ArrayList<>());
                }
                ticket.getScreenshotUrls().add(dataUrl);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Auto-Upload",
                                "Bild '" + file.getFileName() + "' hinzugefügt"));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                "Bild konnte nicht verarbeitet werden: " + e.getMessage()));
            }
        }
    }

    public void handleFileUpload() {
        if (uploadedFile != null && uploadedFile.getContent() != null && uploadedFile.getSize() > 0) {
            try {
                // Konvertierung des Uploads in einen Base64 String zur direkten Speicherung in
                // MongoDB
                String encodedString = Base64.getEncoder().encodeToString(uploadedFile.getContent());
                String contentType = uploadedFile.getContentType();
                String dataUrl = "data:" + contentType + ";base64," + encodedString;

                // Falls die Liste aus irgendeinem Grund noch nicht initialisiert wurde
                if (ticket.getScreenshotUrls() == null) {
                    ticket.setScreenshotUrls(new java.util.ArrayList<>());
                }

                // Zur Liste hinzufügen (Vorschau erfolgt über AJAX-Update in der UI)
                ticket.getScreenshotUrls().add(dataUrl);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                                "Bild '" + uploadedFile.getFileName() + "' wurde der Liste hinzugefügt"));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                "Datei konnte nicht verarbeitet werden: " + e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis", "Keine Datei zum Hochladen ausgewählt."));
        }
    }

    /**
     * Speichert die am Ticket vorgenommenen Änderungen.
     */
    public String update() {
        // Zugewiesenen Benutzer setzen, falls ausgewählt
        if (assignedToUserId != null && !assignedToUserId.isEmpty()) {
            assignableUsers.stream()
                    .filter(u -> u.getId().equals(assignedToUserId))
                    .findFirst()
                    .ifPresent(assignedUser -> {
                        ticket.setAssignedTo(assignedUser.getId());
                        ticket.setAssignedToName(assignedUser.getName());
                    });
        } else {
            // Wenn nichts ausgewählt ist, Zuweisung aufheben
            ticket.setAssignedTo(null);
            ticket.setAssignedToName(null);
        }

        try {
            ticketService.updateTicket(ticket);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Ticket wurde aktualisiert"));
            return "tickets?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler beim Speichern", e.getMessage()));
            return null;
        }
    }

    /**
     * Löscht das aktuelle Ticket permanent.
     */
    public String delete() {
        try {
            ticketService.deleteTicket(ticket.getId());
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Gelöscht", "Ticket wurde erfolgreich entfernt"));
            return "tickets?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler beim Löschen", e.getMessage()));
            return null;
        }
    }

    /**
     * Entfernt einen spezifischen Screenshot vom Ticket.
     */
    public void removeScreenshot(String url) {
        if (ticket != null && ticket.getScreenshotUrls() != null) {
            ticket.getScreenshotUrls().remove(url);
            ticketService.updateTicket(ticket);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Screenshot entfernt",
                            "Das Bild wurde vom Ticket gelöscht."));
        }
    }

    // Hilfsmethoden für die Auswahlmenüs in der UI
    public Ticket.TicketStatus[] getStatuses() {
        return Ticket.TicketStatus.values();
    }

    public Ticket.TimeUnit[] getTimeUnits() {
        return Ticket.TimeUnit.values();
    }

    /**
     * Setzt das Datum für den SCM-Checkin, wenn der Status auf 'wahr' gesetzt wird.
     */
    public void onScmToggle() {
        if (ticket != null) {
            if (ticket.isCheckedInScm()) {
                ticket.setCheckedInAt(java.time.LocalDateTime.now());
            } else {
                ticket.setCheckedInAt(null);
            }
        }
    }
}
