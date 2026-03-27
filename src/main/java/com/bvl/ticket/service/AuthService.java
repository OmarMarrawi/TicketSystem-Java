package com.bvl.ticket.service;

import com.bvl.ticket.model.User;
import com.bvl.ticket.model.UserRole;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@ApplicationScoped
/**
 * @author Omar
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Datenbank-Feldnamen
    private static final String F_ID = "id";
    private static final String F_EMAIL = "email";
    private static final String F_PASSWORD = "password";
    private static final String F_NAME = "name";
    private static final String F_ROLE = "role";
    private static final String F_CREATED_AT = "created_at";

    @Inject
    private MongoService mongoService;

    /**
     * Initialisiert die Datenbank beim ersten Start.
     */
    @javax.annotation.PostConstruct
    public void init() {
        try {
            MongoCollection<Document> users = mongoService.getUsersCollection();
            if (users.countDocuments() == 0) {
                logger.info("Keine Nutzer gefunden. Erstelle Standard-Accounts...");

                // 1. Admin erstellen
                createUser(users, "admin@bvl.bund.de", "admin", "404 Entwickler", UserRole.ADMIN);

                // 2. Referats-Nutzer 401 bis 405 erstellen
                for (int i = 401; i <= 405; i++) {
                    String refNo = String.valueOf(i);
                    createUser(users, refNo + "@bvl.bund.de", refNo, "Mitarbeiter Referat " + refNo,
                            UserRole.valueOf("REFERAT" + refNo));
                }
                logger.info("Standard-Nutzer erfolgreich angelegt.");
            }
        } catch (Exception e) {
            logger.error("Fehler bei der Datenbank-Initialisierung", e);
        }
    }

    private void createUser(MongoCollection<Document> collection, String email, String password, String name,
            UserRole role) {
        Document user = new Document();
        user.append(F_ID, UUID.randomUUID().toString());
        user.append(F_EMAIL, email);
        user.append(F_PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt()));
        user.append(F_NAME, name);
        user.append(F_ROLE, role.name());
        user.append(F_CREATED_AT, LocalDateTime.now().toString());
        collection.insertOne(user);
    }

    public User login(String email, String password) {
        try {
            MongoCollection<Document> users = mongoService.getUsersCollection();
            Document userDoc = users.find(Filters.eq(F_EMAIL, email)).first();

            if (userDoc != null) {
                String hashedPassword = userDoc.getString(F_PASSWORD);
                if (BCrypt.checkpw(password, hashedPassword)) {
                    logger.info("Login erfolgreich für: {}", email);
                    return mapDocumentToUser(userDoc);
                }
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
        MongoCollection<Document> users = mongoService.getUsersCollection();

        // Prüfen, ob die E-Mail bereits existiert
        if (users.find(Filters.eq(F_EMAIL, email)).first() != null) {
            throw new IllegalArgumentException("Ein Benutzer mit dieser E-Mail-Adresse existiert bereits.");
        }

        // Passwort hashen
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Benutzer-Dokument erstellen
        Document newUserDoc = new Document();
        newUserDoc.append(F_ID, UUID.randomUUID().toString());
        newUserDoc.append(F_EMAIL, email);
        newUserDoc.append(F_PASSWORD, hashedPassword);
        newUserDoc.append(F_NAME, name);
        newUserDoc.append(F_ROLE, role.name());
        newUserDoc.append(F_CREATED_AT, LocalDateTime.now().toString());

        // Benutzer in die Datenbank einfügen
        users.insertOne(newUserDoc);
        logger.info("Neuer Benutzer registriert: {}", email);

        return mapDocumentToUser(newUserDoc);
    }

    public java.util.List<User> getAllUsers() {
        java.util.List<User> userList = new java.util.ArrayList<>();
        try {
            MongoCollection<Document> users = mongoService.getUsersCollection();
            for (Document doc : users.find()) {
                userList.add(mapDocumentToUser(doc));
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden aller Benutzer", e);
        }
        return userList;
    }

    private User mapDocumentToUser(Document doc) {
        User user = new User();
        user.setId(doc.getString(F_ID));
        user.setEmail(doc.getString(F_EMAIL));
        user.setName(doc.getString(F_NAME));
        user.setRole(UserRole.valueOf(doc.getString(F_ROLE).toUpperCase()));

        Object createdAt = doc.get(F_CREATED_AT);
        if (createdAt instanceof String) {
            user.setCreatedAt(LocalDateTime.parse((String) createdAt));
        } else if (createdAt instanceof Date) {
            user.setCreatedAt(((Date) createdAt).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return user;
    }
}
