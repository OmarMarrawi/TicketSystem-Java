package com.bvl.ticket.service;

import com.bvl.ticket.model.Vacation;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class VacationService {

    private static final Logger logger = LoggerFactory.getLogger(VacationService.class);

    @Inject
    private DatabaseService database;

    public List<Vacation> getAllVacations() {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT v FROM Vacation v ORDER BY v.startDate DESC, v.createdAt DESC", Vacation.class)
                    .getResultList());
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Urlaubseinträge", e);
            return new ArrayList<>();
        }
    }

    public List<Vacation> getVacationsForUser(Long userId) {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT v FROM Vacation v WHERE v.userId = :userId ORDER BY v.startDate DESC, v.createdAt DESC", Vacation.class)
                    .setParameter("userId", userId)
                    .getResultList());
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Urlaubseinträge für Benutzer {}", userId, e);
            return new ArrayList<>();
        }
    }

    public void save(Vacation vacation) {
        vacation.setCreatedAt(LocalDateTime.now());
        vacation.setUpdatedAt(LocalDateTime.now());
        database.run(em -> em.persist(vacation));
    }

    public void update(Vacation vacation) {
        vacation.setUpdatedAt(LocalDateTime.now());
        database.run(em -> em.merge(vacation));
    }

    public void delete(Long id) {
        database.run(em -> {
            Vacation vacation = em.find(Vacation.class, id);
            if (vacation != null) {
                em.remove(vacation);
            }
        });
    }
}
