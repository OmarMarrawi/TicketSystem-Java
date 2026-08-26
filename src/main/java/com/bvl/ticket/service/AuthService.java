package com.bvl.ticket.service;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.UserRole;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
/**
 * @author Omar
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Inject
    private DatabaseService database;

    /**
     * Initialisiert die Datenbank beim ersten Start.
     */
    @javax.annotation.PostConstruct
    public void init() {
        try {
            Long count = database.read(em -> em.createQuery(
                    "SELECT COUNT(u) FROM User u", Long.class).getSingleResult());

            if (count == null || count == 0) {
                logger.info("Keine Nutzer gefunden. Erstelle Standard-Accounts...");

                // 1. Admin erstellen
                createUser("admin@bvl.bund.de", "admin", "404 Entwickler", UserRole.ADMIN);

                // 2. Referats-Nutzer 401 bis 405 erstellen
                for (int i = 401; i <= 405; i++) {
                    String refNo = String.valueOf(i);
                    createUser(refNo + "@bvl.bund.de", refNo, "Mitarbeiter Referat " + refNo,
                            UserRole.valueOf("REFERAT" + refNo));
                }
                logger.info("Standard-Nutzer erfolgreich angelegt.");
            }
        } catch (Exception e) {
            logger.error("Fehler bei der Datenbank-Initialisierung", e);
        }
    }

    private void createUser(String email, String password, String name, UserRole role) {
        database.run(em -> {
            User user = new User();
            user.setEmail(email);
            user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
            user.setName(name);
            user.setRole(role);
            em.persist(user);
        });
    }

    public User login(String email, String password) {
        try {
            User user = findUserByEmail(email);
            if (user != null && BCrypt.checkpw(password, user.getPassword())) {
                logger.info("Login erfolgreich für: {}", email);
                return user;
            }
            logger.warn("Login-Versuch fehlgeschlagen für: {}", email);
        } catch (Exception e) {
            logger.error("Datenbankfehler während des Logins für: {}", email, e);
        }
        return null;
    }

    /**
     * Registriert einen neuen Benutzer im System.
     *
     * @param email Die E-Mail-Adresse des Benutzers (muss eindeutig sein).
     * @param password Das Klartext-Passwort des Benutzers.
     * @param name Der Anzeigename des Benutzers.
     * @param role Die Rolle des Benutzers.
     * @return Der neu erstellte Benutzer.
     * @throws IllegalArgumentException Wenn ein Benutzer mit der angegebenen E-Mail bereits existiert.
     */
    public User registerUser(String email, String password, String name, UserRole role) {
        // Prüfen, ob die E-Mail bereits existiert
        if (findUserByEmail(email) != null) {
            throw new IllegalArgumentException("Ein Benutzer mit dieser E-Mail-Adresse existiert bereits.");
        }

        // Benutzer anlegen und Passwort hashen
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        newUser.setName(name);
        newUser.setRole(role);

        database.run(em -> em.persist(newUser));
        logger.info("Neuer Benutzer registriert: {}", email);

        return newUser;
    }

    public java.util.List<User> getAllUsers() {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT u FROM User u ORDER BY u.createdAt ASC", User.class)
                    .getResultList());
        } catch (Exception e) {
            logger.error("Fehler beim Laden aller Benutzer", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Legt einen neuen Benutzer an oder aktualisiert einen bestehenden.
     * Wenn ein neues Passwort übergeben wird, wird es gehasht gespeichert.
     *
     * @throws IllegalArgumentException Wenn die E-Mail bereits von einem anderen Benutzer verwendet wird.
     */
    public void updateUser(User user, String newPassword) {
        User existing = findUserByEmail(user.getEmail());
        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Ein Benutzer mit dieser E-Mail-Adresse existiert bereits.");
        }

        final boolean isNew = (user.getId() == null);
        database.run(em -> {
            if (newPassword != null && !newPassword.isEmpty()) {
                user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            }
            if (isNew) {
                em.persist(user);
            } else {
                em.merge(user);
            }
        });

        if (isNew) {
            logger.info("Neuer Benutzer erstellt: {} (ID {})", user.getEmail(), user.getId());
        } else {
            logger.info("Benutzer aktualisiert: {} (ID {})", user.getEmail(), user.getId());
        }
    }

    /**
     * Setzt ein neues Passwort für den Benutzer (wird gehasht gespeichert).
     */
    public void updatePassword(Long userId, String newPassword) {
        database.run(em -> {
            User user = em.find(User.class, userId);
            if (user != null) {
                user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            }
        });
        logger.info("Passwort zurückgesetzt für Benutzer-ID {}", userId);
    }

    /**
     * Löscht einen Benutzer permanent.
     */
    public void deleteUser(Long userId) {
        database.run(em -> {
            User user = em.find(User.class, userId);
            if (user != null) {
                em.remove(user);
            }
        });
        logger.info("Benutzer mit ID {} gelöscht", userId);
    }

    private User findUserByEmail(String email) {
        try {
            return database.read(em -> em.createQuery(
                    "SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult());
        } catch (NoResultException e) {
            return null;
        }
    }
}
