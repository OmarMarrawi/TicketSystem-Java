# TicketSystem Projektkontext

Diese Datei dient als kompakte Referenz, damit das Projekt nicht bei jeder Aufgabe neu voll analysiert werden muss.

## Projektueberblick
- Name: `ticket-system`
- Typ: Java EE Webanwendung mit JSF/PrimeFaces
- Build: Maven (`war`)
- Java: 8
- Deployment: Payara Micro (laut `Dockerfile`)

## Tech-Stack
- Java EE API 8 (`javax:javaee-api`, scope `provided`)
- JSF + PrimeFaces + OmniFaces
- PostgreSQL 16 + Hibernate ORM 5.6 (JPA 2.2, `RESOURCE_LOCAL`)
- Lombok
- Jackson
- jBCrypt
- SLF4J + Logback
- Apache POI (Excel Export)

## Wichtige Verzeichnisse
- `src/main/java/com/bvl/ticket/model` - Domainenmodelle
- `src/main/java/com/bvl/ticket/service` - Service-Schicht (DB, Auth, Tickets)
- `src/main/java/com/bvl/ticket/bean` - JSF/CDI Managed Beans (UI-Logik)
- `src/main/webapp` - XHTML Seiten, statische Assets
- `src/main/webapp/WEB-INF` - Web-/JSF-/CDI-Konfiguration

## Zentrale Klassen
- `DatabaseService` - PostgreSQL/Hibernate Verbindung und Transaktions-Helper
- `AuthService` - Login, Registrierung, Benutzerverwaltung
- `TicketService` - Ticket-CRUD, Filter, Dashboard-Statistiken
- `LoginBean` - Login-Flow und Session
- `TicketBean` - Ticketliste, Filter, Export
- `CreateTicketBean` - Ticketanlage
- `TicketDetailBean` - Detailansicht, Update/Delete, Upload
- `AdminBean` - Admin-Dashboard

## Domainenmodell
- `User`
- `Ticket`
- `UserRole` (u. a. `ADMIN`, Referat-Rollen)
- `DashboardStats`

## Einstieg und Request-Flow
- Einstieg ueber Webcontainer (kein `main()` im Projekt)
- Servlet-Mapping: `*.xhtml` in `WEB-INF/web.xml`
- Welcome-File: `login.xhtml`
- Typischer Ablauf: Login -> Dashboard -> Ticketliste -> Ticketdetail / Ticket anlegen

## Konfiguration
- PostgreSQL URL:
  - Env: `DB_URL`
  - Fallback System-Property: `DB_URL`
  - Default: `jdbc:postgresql://localhost:5432/ticket_system_db`
- Datenbank-Benutzer:
  - Env/System-Property: `DB_USER`, Default: `postgres`
  - Env/System-Property: `DB_PASSWORD`, Default: `zag`
  - Lokale PostgreSQL 16, Datenbank `ticket_system_db` bereits angelegt
- JPA Unit: `ticketPU` (`src/main/resources/META-INF/persistence.xml`)
- Schema wird automatisch erzeugt/aktualisiert (`hibernate.hbm2ddl.auto=update`)
- JSF Stage: `Development`
- PrimeFaces Theme: `saga`

## Build und Run
- Lokaler Build: `mvn clean package`
- Artefakt: `target/ticket-system.war`
- Docker: Multi-Stage Build (Maven + Payara Micro)
- Deployment (später): Plan in `DEPLOYMENT.md` (Koyeb + Neon, kostenlos)

## Tests
- Aktuell kein `src/test` gefunden
- Keine explizite Test-Suite im Maven-Setup hinterlegt

## Schnellreferenz fuer zukuenftige Aufgaben
Bei neuen Aufgaben zuerst diese Datei lesen und nur bei Bedarf gezielt in folgende Dateien schauen:
- Build/Dependencies: `pom.xml`
- Laufzeit/Web-Konfig: `src/main/webapp/WEB-INF/web.xml`
- DB-Konfig: `src/main/java/com/bvl/ticket/service/DatabaseService.java` und `src/main/resources/META-INF/persistence.xml`
- Geschaeftslogik Tickets: `src/main/java/com/bvl/ticket/service/TicketService.java`
- Auth-Flow: `src/main/java/com/bvl/ticket/service/AuthService.java`

## Pflegehinweis
Wenn neue Module, Rollen, Seiten oder Services dazukommen, diese Datei kurz aktualisieren.
