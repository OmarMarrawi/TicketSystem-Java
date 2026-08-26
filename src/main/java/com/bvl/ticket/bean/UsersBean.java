package com.bvl.ticket.bean;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.UserRole;
import com.bvl.ticket.service.AuthService;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
/**
 * @author Omar
 */
public class UsersBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(UsersBean.class);
    private static final long serialVersionUID = 1L;

    @Inject
    private AuthService authService;

    @Inject
    private LoginBean loginBean;

    @Getter
    private List<User> users;

    @Getter
    @Setter
    private User selectedUser;

    @Getter
    @Setter
    private String newPassword;

    @Getter
    private boolean editMode;

    @PostConstruct
    public void init() {
        selectedUser = new User();
        reload();
    }

    private void reload() {
        users = authService.getAllUsers();
    }

    public UserRole[] getRoles() {
        return UserRole.values();
    }

    /**
     * Öffnet den Dialog zum Anlegen eines neuen Benutzers.
     */
    public void prepareNew() {
        selectedUser = new User();
        newPassword = "";
        editMode = false;
    }

    /**
     * Öffnet den Dialog zum Bearbeiten eines bestehenden Benutzers.
     */
    public void prepareEdit(User user) {
        // Arbeitskopie, damit ein Abbrechen die Tabelle nicht verändert
        selectedUser = new User();
        selectedUser.setId(user.getId());
        selectedUser.setEmail(user.getEmail());
        selectedUser.setName(user.getName());
        selectedUser.setRole(user.getRole());
        selectedUser.setPassword(user.getPassword());
        newPassword = "";
        editMode = true;
    }

    /**
     * Speichert den neuen oder bearbeiteten Benutzer.
     */
    public void save() {
        if (selectedUser == null) {
            return;
        }

        boolean hasPassword = newPassword != null && !newPassword.isEmpty();

        // Neue Benutzer brauchen zwingend ein Start-Passwort
        if (!editMode && !hasPassword) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Bitte ein Start-Passwort angeben");
            return;
        }

        try {
            authService.updateUser(selectedUser, newPassword);

            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                    "Benutzer '" + selectedUser.getEmail() + "' wurde gespeichert");
            reload();
            PrimeFaces.current().executeScript("PF('userDialog').hide()");
        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", e.getMessage());
        } catch (Exception e) {
            logger.error("Fehler beim Speichern des Benutzers", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer konnte nicht gespeichert werden");
        }
    }

    /**
     * Löscht einen Benutzer. Der eigene Account kann nicht gelöscht werden.
     */
    public void deleteUser(User user) {
        if (loginBean.getCurrentUser() != null
                && user.getId().equals(loginBean.getCurrentUser().getId())) {
            addMessage(FacesMessage.SEVERITY_WARN, "Nicht möglich",
                    "Der eigene Account kann nicht gelöscht werden");
            return;
        }
        try {
            authService.deleteUser(user.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Gelöscht",
                    "Benutzer '" + user.getEmail() + "' wurde entfernt");
            reload();
        } catch (Exception e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Benutzer konnte nicht gelöscht werden");
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, detail));
    }
}
