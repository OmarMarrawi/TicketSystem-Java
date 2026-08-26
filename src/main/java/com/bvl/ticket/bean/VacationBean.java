package com.bvl.ticket.bean;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.Vacation;
import com.bvl.ticket.service.VacationService;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Getter;
import lombok.Setter;

@Named
@ViewScoped
public class VacationBean implements Serializable {

    @Inject
    private VacationService vacationService;

    @Inject
    private LoginBean loginBean;

    @Getter
    private List<Vacation> vacations = new ArrayList<>();

    @Getter
    @Setter
    private Vacation vacation;

    @Getter
    @Setter
    private boolean editing;

    @PostConstruct
    public void init() {
        refresh();
        prepareNew();
    }

    public void refresh() {
        User user = loginBean.getCurrentUser();
        if (user == null) {
            vacations = new ArrayList<>();
        } else {
            // Alle angemeldeten Mitarbeitenden sehen die gemeinsame Urlaubsplanung.
            vacations = vacationService.getAllVacations();
        }
    }

    public void prepareNew() {
        vacation = new Vacation();
        vacation.setStartDate(LocalDate.now());
        vacation.setEndDate(LocalDate.now());
        editing = false;
    }

    public void edit(Vacation entry) {
        if (!canManage(entry)) {
            denyAccess();
            return;
        }
        vacation = entry;
        editing = true;
    }

    public void save() {
        if (vacation.getStartDate() == null || vacation.getEndDate() == null) {
            message(FacesMessage.SEVERITY_ERROR, "Zeitraum fehlt", "Bitte Start- und Enddatum angeben.");
            return;
        }
        if (vacation.getEndDate().isBefore(vacation.getStartDate())) {
            message(FacesMessage.SEVERITY_ERROR, "Ungültiger Zeitraum", "Das Enddatum darf nicht vor dem Startdatum liegen.");
            return;
        }

        try {
            User user = loginBean.getCurrentUser();
            if (!editing) {
                vacation.setUserId(user.getId());
                vacation.setUserName(user.getName());
                vacationService.save(vacation);
                message(FacesMessage.SEVERITY_INFO, "Urlaub erfasst", "Der Urlaubseintrag wurde gespeichert.");
            } else {
                if (!canManage(vacation)) {
                    denyAccess();
                    return;
                }
                vacationService.update(vacation);
                message(FacesMessage.SEVERITY_INFO, "Urlaub aktualisiert", "Der Urlaubseintrag wurde geändert.");
            }
            refresh();
            prepareNew();
        } catch (Exception e) {
            message(FacesMessage.SEVERITY_ERROR, "Speichern fehlgeschlagen", "Der Urlaubseintrag konnte nicht gespeichert werden.");
        }
    }

    public void delete(Vacation entry) {
        if (!canManage(entry)) {
            denyAccess();
            return;
        }
        try {
            vacationService.delete(entry.getId());
            refresh();
            prepareNew();
            message(FacesMessage.SEVERITY_INFO, "Urlaub gelöscht", "Der Urlaubseintrag wurde entfernt.");
        } catch (Exception e) {
            message(FacesMessage.SEVERITY_ERROR, "Löschen fehlgeschlagen", "Der Urlaubseintrag konnte nicht gelöscht werden.");
        }
    }

    public boolean canManage(Vacation entry) {
        User user = loginBean.getCurrentUser();
        return user != null && user.getId().equals(entry.getUserId());
    }

    public long getDurationDays(Vacation entry) {
        return ChronoUnit.DAYS.between(entry.getStartDate(), entry.getEndDate()) + 1;
    }

    public String formatDate(LocalDate date) {
        return date == null ? "" : date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private void denyAccess() {
        message(FacesMessage.SEVERITY_ERROR, "Keine Berechtigung", "Sie können diesen Urlaubseintrag nicht bearbeiten.");
    }

    private void message(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
