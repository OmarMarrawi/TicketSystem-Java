package com.bvl.ticket.bean;

import com.bvl.ticket.model.User;
import com.bvl.ticket.service.AuthService;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import java.io.Serializable;

@Named
@SessionScoped
/**
 * @author Omar
 */
public class LoginBean implements Serializable {

    @Getter
    @Setter
    private String email;

    @Getter
    @Setter
    private String password;

    @Getter
    private User currentUser;

    @Inject
    private AuthService authService;

    /**
     * Führt den Login-Prozess aus.
     * Leitet bei Erfolg zum Dashboard weiter.
     */
    public String login() {
        currentUser = authService.login(email, password);
        if (currentUser != null) {
            return "dashboard?faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Login fehlgeschlagen",
                            "Ungültige E-Mail oder Passwort"));
            return null;
        }
    }

    /**
     * Meldet den Benutzer ab und zerstört die Session.
     */
    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "login?faces-redirect=true";
    }

    /**
     * Prüft, ob ein Benutzer aktuell angemeldet ist.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}



