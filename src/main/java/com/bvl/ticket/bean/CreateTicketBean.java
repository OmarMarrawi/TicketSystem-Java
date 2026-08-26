package com.bvl.ticket.bean;

import com.bvl.ticket.model.Ticket;
import com.bvl.ticket.model.User;
import com.bvl.ticket.service.TicketService;
import com.bvl.ticket.service.AuthService;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Base64;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.event.FileUploadEvent;

@Named
@ViewScoped
/**
 * @author Omar
 */
public class CreateTicketBean implements Serializable {

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
    private AuthService authService; // Inject AuthService

    @Getter
    private java.util.List<User> assignableUsers; // Liste der zuweisbaren Benutzer

    @Getter
    @Setter
    private Long assignedToUserId; // ID des ausgewählten Benutzers

    /**
     * Verarbeitet den Datei-Upload während der Erstellung.
     * 1. Prüft, ob eine Datei ausgewählt wurde.
     * 2. Wandelt den binären Inhalt in einen Base64-String um.
     * 3. Erzeugt eine Data-URL (z.B. data:image/png;base64,...), die direkt im
     * Browser angezeigt werden kann.
     * 4. Fügt diesen String der Liste screenshotUrls des Tickets hinzu.
     */
    /**
     * Listener für den automatischen Datei-Upload.
     * Wird direkt aufgerufen, sobald ein Bild ausgewählt wurde (auto="true").
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
                // Konvertierung: Byte-Array -> Base64-String
                String encodedString = Base64.getEncoder().encodeToString(uploadedFile.getContent());
                String contentType = uploadedFile.getContentType();

                // Formatierung als Data-URL für die direkte Anzeige (p:graphicImage
                // stream="false")
                String dataUrl = "data:" + contentType + ";base64," + encodedString;

                // Sicherheitshalber sicherstellen, dass die Liste existiert
                if (ticket.getScreenshotUrls() == null) {
                    ticket.setScreenshotUrls(new java.util.ArrayList<>());
                }

                // Bild zur Liste im Ticket-Objekt hinzufügen
                ticket.getScreenshotUrls().add(dataUrl);

                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                                "Bild '" + uploadedFile.getFileName() + "' hinzugefügt"));
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                                "Bild konnte nicht verarbeitet werden: " + e.getMessage()));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "Hinweis",
                            "Bitte wählen Sie zuerst eine Bilddatei aus."));
        }
    }

    /**
     * Bereitet ein leeres Ticket-Objekt vor.
     */
    @PostConstruct
    public void init() {
        ticket = new Ticket();
        ticket.setPriority(Ticket.TicketPriority.MITTEL); // Standard-Präferenz
        ticket.setFeatureType(Ticket.FeatureType.TASK);
        assignableUsers = authService.getAllUsers(); // Alle Benutzer laden
    }

    /**
     * Speichert das neue Ticket und ordnet es dem aktuellen Nutzer zu.
     */
    public String save() {
        User user = loginBean.getCurrentUser();
        if (user == null)
            return "login?faces-redirect=true";

        // Nutzer-Metadaten am Ticket setzen
        ticket.setCreatedBy(user.getId());
        ticket.setCreatedByName(user.getName());
        ticket.setCreatedByRole(user.getRole().getLabel());

        // Zugewiesenen Benutzer setzen, falls ausgewählt
        if (assignedToUserId != null) {
            assignableUsers.stream()
                    .filter(u -> u.getId().equals(assignedToUserId))
                    .findFirst()
                    .ifPresent(assignedUser -> {
                        ticket.setAssignedTo(assignedUser.getId());
                        ticket.setAssignedToName(assignedUser.getName());
                    });
        }

        try {
            ticketService.saveTicket(ticket);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erstellt", "Ticket wurde erfolgreich angelegt"));
            return "tickets?faces-redirect=true";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler",
                            "Ticket konnte nicht gespeichert werden: " + e.getMessage()));
            return null;
        }
    }

    // Hilfsmethoden für Auswahlfelder im Formular
    public Ticket.TicketPriority[] getPriorities() {
        return Ticket.TicketPriority.values();
    }

    public Ticket.FeatureType[] getFeatureTypes() {
        return Ticket.FeatureType.values();
    }

    public Ticket.TimeUnit[] getTimeUnits() {
        return Ticket.TimeUnit.values();
    }
}
