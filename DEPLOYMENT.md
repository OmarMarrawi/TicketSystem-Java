# Deployment-Plan (für später)

Status: **Geplant** – umsetzen, sobald das Projekt fertig entwickelt ist.
Ziel: Kostenloses öffentliches Hosting der App + PostgreSQL-Datenbank.

## Gewählte Variante: Koyeb (App) + Neon (Datenbank)

### 1. Datenbank bei Neon.tech anlegen
1. Account auf https://neon.tech erstellen (kostenlos, keine Kreditkarte)
2. Neues Projekt anlegen -> Region nah wählen (z.B. EU/Frankfurt)
3. Connection String kopieren, Format:
   `postgresql://BENUTZER:PASSWORT@HOST/ticket_system_db?sslmode=require`
4. Für die App als Umgebungsvariablen verwenden:
   - DB_URL=jdbc:postgresql://HOST/ticket_system_db?sslmode=require
   - DB_USER=...
   - DB_PASSWORD=...
   - Wichtig: JDBC-Präfix `jdbc:` ergänzen und `?sslmode=require` behalten
   - Der Code liest diese Variablen bereits (DatabaseService.java)

### 2. App bei Koyeb.com deployen
1. Projekt nach GitHub pushen
2. Auf Koyeb: Create Service -> GitHub-Repo verbinden -> "Docker" Build-Typ wählen
3. Umgebungsvariablen setzen (DB_URL, DB_USER, DB_PASSWORD)
4. Port 8080 exponieren (Koyeb leitet HTTPS davor)
5. Free Tier: 1 Service, 512 MB RAM - JVM_ARGS im Dockerfile steht schon auf 256 MB Heap

### Alternativen (falls Koyeb nicht passt)
- Render.com: Free Web Service (schläft nach 15 Min Inaktivität), Free Postgres nur 30 Tage
- Google Cloud Run: sehr großzügiges Free-Kontingent, Kreditkarte erforderlich
- Back4App Containers: Free Tier für Container
- Oracle Cloud "Always Free": echte VMs dauerhaft gratis (Kreditkarte zur Verifizierung), darauf alles selbst installierbar

## Vorher noch anzupassen (Checkliste)
- [ ] hbm2ddl.auto=update in DatabaseService.java fuer Produktion ueberdenken
      (erstmal ok, langfristig kontrolliertes Schema-Migration z.B. Flyway)
- [ ] Default-Passwort 'zag' aus DatabaseService.java entfernen,
      nur noch ueber Env-Vars (kein hartkodiertes Passwort oeffentlich!)
- [ ] JSF PROJECT_STAGE von 'Development' auf 'Production' (web.xml)
- [ ] Pruefen, ob Payara Micro auf $PORT reagieren muss (Cloud Run ja, Koyeb konfigurierbar)
- [ ] GitHub-Repo vorbereiten (keine Passwoerter committen!)

## Hinweise
- GitHub Pages / Vercel / Netlify funktionieren NICHT (nur statische Seiten)
- Railway hat keinen echten Free-Tier mehr
