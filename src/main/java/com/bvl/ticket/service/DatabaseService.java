package com.bvl.ticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Zentrale Verwaltung der PostgreSQL-Verbindung über Hibernate/JPA.
 * Ersetzt den früheren MongoService.
 */
@ApplicationScoped
/**
 * @author Omar
 */
public class DatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    private EntityManagerFactory emf;

    /**
     * Baut die EntityManagerFactory auf.
     * Konfiguration über Umgebungsvariablen (analog zum früheren MONGO_URL-Muster):
     * - DB_URL      (Default: jdbc:postgresql://localhost:5432/ticket_system_db)
     * - DB_USER     (Default: postgres)
     * - DB_PASSWORD (Default: zag)
     */
    @PostConstruct
    public void init() {
        String dbUrl = getSetting("DB_URL", "jdbc:postgresql://localhost:5432/ticket_system_db");
        String dbUser = getSetting("DB_USER", "postgres");
        String dbPassword = getSetting("DB_PASSWORD", "zag");

        logger.info("Verbinde zu PostgreSQL: {} (Benutzer: {})", dbUrl, dbUser);

        try {
            Map<String, Object> props = new HashMap<>();
            props.put("javax.persistence.jdbc.url", dbUrl);
            props.put("javax.persistence.jdbc.user", dbUser);
            props.put("javax.persistence.jdbc.password", dbPassword);
            props.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL10Dialect");
            props.put("hibernate.hbm2ddl.auto", "update");
            props.put("hibernate.show_sql", false);
            props.put("hibernate.format_sql", true);

            emf = Persistence.createEntityManagerFactory("ticketPU", props);
            logger.info("PostgreSQL Verbindung erfolgreich hergestellt.");
        } catch (Exception e) {
            logger.error("Kritischer Fehler bei der PostgreSQL-Verbindung", e);
        }
    }

    /**
     * Liest zuerst die Umgebungsvariable, dann die System-Property.
     */
    private String getSetting(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty())
            value = System.getProperty(key, defaultValue);
        return value;
    }

    @PreDestroy
    public void cleanup() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    /**
     * Führt eine Funktion mit einem EntityManager in einer Transaktion aus
     * und gibt deren Ergebnis zurück.
     */
    public <T> T call(Function<EntityManager, T> function) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            T result = function.apply(em);
            em.getTransaction().commit();
            return result;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Führt eine Aktion mit einem EntityManager in einer Transaktion aus.
     */
    public void run(Consumer<EntityManager> action) {
        call(em -> {
            action.accept(em);
            return null;
        });
    }

    /**
     * Führt eine reine Lese-Aktion ohne Schreib-Transaktion aus.
     */
    public <T> T read(Function<EntityManager, T> function) {
        EntityManager em = emf.createEntityManager();
        try {
            return function.apply(em);
        } finally {
            em.close();
        }
    }
}
