package com.bvl.ticket.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.MongoClientSettings;
import com.mongodb.ConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.bson.Document;

@ApplicationScoped
/**
 * @author Omar
 */
public class MongoService {

    private static final Logger logger = LoggerFactory.getLogger(MongoService.class);

    private MongoClient mongoClient;

    @Getter
    private MongoDatabase database;

    /**
     * Stellt die Verbindung zur MongoDB her.
     * Prüft Umgebungsvariablen für die MONGO_URL.
     */
    @PostConstruct
    public void init() {
        String mongoUrl = System.getenv("MONGO_URL");
        if (mongoUrl == null)
            mongoUrl = System.getProperty("MONGO_URL", "mongodb://localhost:27017");

        String dbName = System.getenv("DB_NAME");
        if (dbName == null)
            dbName = System.getProperty("DB_NAME", "ticket_system_db");

        logger.info("Verbinde zu MongoDB: {} (Datenbank: {})", mongoUrl, dbName);

        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoUrl))
                    .applyToClusterSettings(builder -> builder.serverSelectionTimeout(3, TimeUnit.SECONDS))
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(dbName);
            logger.info("MongoDB Verbindung erfolgreich hergestellt.");
        } catch (Exception e) {
            logger.error("Kritischer Fehler bei der MongoDB-Verbindung", e);
        }
    }

    /**
     * Schließt die Verbindung beim Beenden der Anwendung.
     */
    @PreDestroy
    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Zugriff auf die Nutzer-Sammlung.
     */
    public MongoCollection<Document> getUsersCollection() {
        return database.getCollection("users");
    }

    /**
     * Zugriff auf die Ticket-Sammlung.
     */
    public MongoCollection<Document> getTicketsCollection() {
        return database.getCollection("tickets");
    }
}



