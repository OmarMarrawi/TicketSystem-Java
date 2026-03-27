package com.bvl.ticket.bean;

import com.bvl.ticket.model.UserRole;
import com.bvl.ticket.service.AuthService;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import javax.annotation.PostConstruct;

@Named
@RequestScoped
/**
 * @author Omar
 */
public class RegisterBean implements Serializable {

    @Getter @Setter
    private String email;

    @Getter @Setter
    private String name;

    @Getter @Setter
    private String password;

    @Getter @Setter
    private String confirmPassword;

    @Getter @Setter
    private UserRole selectedRole; // Für die Rollenauswahl im Formular

    @Getter
    private List<UserRole> availableRoles; // Liste der verfügbaren Rollen

    @Inject
    private AuthService authService;

    @PostConstruct
    public void init() {
        availableRoles = Arrays.asList(UserRole.values());
    }

    /**
     * Führt den Registrierungsprozess aus.
     * Leitet bei Erfolg zum Login weiter.
     */
    public String register() {
        FacesContext context = FacesContext.getCurrentInstance();

        // 1. Validierung der Passwörter
        if (!password.equals(confirmPassword)) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Passwörter stimmen nicht überein!"));
            return null;
        }

        // 2. Validierung der ausgewählten Rolle
        if (selectedRole == null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Bitte wählen Sie eine Rolle aus!"));
            return null;
        }

        try {
            // 3. Benutzer registrieren mit der ausgewählten Rolle
            authService.registerUser(email, password, name, selectedRole);

            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolgreich", "Registrierung erfolgreich! Bitte melden Sie sich an."));
            return "login?faces-redirect=true"; // Weiterleitung zur Login-Seite
        } catch (IllegalArgumentException e) {
            // Fehler, wenn z.B. E-Mail bereits existiert
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", e.getMessage()));
            return null;
        } catch (Exception e) {
            // Allgemeine Fehlerbehandlung
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Registrierung fehlgeschlagen: " + e.getMessage()));
            return null;
        }
    }
}
