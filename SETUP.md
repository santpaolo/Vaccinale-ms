# Setup e Installazione vaccinale-ms

## Indice
- [Prerequisiti](#prerequisiti)
- [Installazione Locale](#installazione-locale)
- [Configurazione](#configurazione)
- [Build](#build)
- [Esecuzione](#esecuzione)
- [Docker](#docker)
- [Troubleshooting](#troubleshooting)

---

## Prerequisiti

### Software Richiesto

| Software | Versione | Note |
|----------|----------|------|
| **Java JDK** | 11+ | OpenJDK 11 consigliato |
| **Maven** | 3.6.0+ | Incluso Maven Wrapper (mvnw) |
| **PostgreSQL** | 12+ | Database principale |
| **RabbitMQ** | 3.8+ | Message broker |
| **MinIO** | RELEASE.2022+ | Object storage (S3-compatible) |
| **Docker** | 20.10+ | (Opzionale) Per deployment containerizzato |
| **Git** | 2.x | Version control |

### IDE Consigliati

- **IntelliJ IDEA** (configurazione già presente in `.idea/`)
- **Eclipse** (configurazione già presente in `.project`, `.classpath`)
- **VS Code** con estensioni Java

---

## Installazione Locale

### 1. Clone del Repository

```bash
git clone <repository-url>
cd vaccinale-ms
```

### 2. Installazione Dipendenze Maven

Il progetto include **Maven Wrapper** per garantire la versione corretta:

**Windows:**
```bash
mvnw.cmd clean install
```

**Linux/Mac:**
```bash
./mvnw clean install
```

**Se Maven è già installato:**
```bash
mvn clean install
```

### 3. Setup Database PostgreSQL

#### Crea Database e Schema

```sql
-- Connessione come superuser
psql -U postgres

-- Crea database
CREATE DATABASE vaccinale_db;

-- Connessione al database
\c vaccinale_db

-- Crea schema
CREATE SCHEMA vacc20;

-- Crea utente applicativo (opzionale)
CREATE USER vacc_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON SCHEMA vacc20 TO vacc_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA vacc20 TO vacc_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA vacc20 TO vacc_user;
```

#### Applica Migrations

Il progetto usa `ddl-auto: none`, quindi le tabelle devono essere create esternamente.

**Opzioni:**
1. **Flyway/Liquibase** (se presente in altro progetto)
2. **Script SQL manuali** (dalla cartella migrations se esistente)
3. **Import da dump esistente**

```bash
# Esempio import da dump
psql -U vacc_user -d vaccinale_db -f schema_vacc20.sql
```

### 4. Setup RabbitMQ

#### Installazione

**Docker (consigliato per sviluppo):**
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3-management
```

**Accesso Management UI:** http://localhost:15672

#### Configurazione Virtual Host

```bash
# Connessione al container
docker exec -it rabbitmq bash

# Crea virtual host
rabbitmqctl add_vhost /vaccini

# Imposta permessi
rabbitmqctl set_permissions -p /vaccini admin ".*" ".*" ".*"
```

#### Exchange e Queue (creati automaticamente)

Il microservizio crea automaticamente:
- `vaccini_job_invio_esito` (exchange direct)
- `email_dispatcher_queue` (routing key)
- `fse_certificato_vaccinale_notification_q`
- `fse_scheda_vaccinazione_deletion_q`
- `fse_scheda_vaccinazione_request_signature_q`

### 5. Setup MinIO (Object Storage)

#### Installazione

**Docker:**
```bash
docker run -d --name minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

**Accesso Console:** http://localhost:9001

#### Crea Bucket

Accedi alla console MinIO e crea i bucket:
- `consensi-bucket` - Consensi firmati
- `anamnesi-bucket` - Dati anamnestici
- `documenti-bucket` - Documenti generici

**Oppure via CLI:**
```bash
# Installa mc (MinIO Client)
# https://min.io/docs/minio/linux/reference/minio-mc.html

mc alias set local http://localhost:9000 minioadmin minioadmin
mc mb local/consensi-bucket
mc mb local/anamnesi-bucket
mc mb local/documenti-bucket
```

---

## Configurazione

### Variabili d'Ambiente

Crea un file `.env` (o configura nell'IDE):

```bash
# Database PostgreSQL
DATABASE_URL_VACC=jdbc:postgresql://localhost:5432/vaccinale_db
DATABASE_USERNAME=vacc_user
DATABASE_PASSWORD=your_password
DATABASE_VACC_MAX_POOLSIZE=10
SHOW_SQL=false

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin
RABBITMQ_VH=/vaccini

# MinIO
MINIO_URL=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_CONSENSI=consensi-bucket
MINIO_BUCKET_ANAMNESI=anamnesi-bucket

# File Upload
MAX_FILE_SIZE=10MB
MAX_REQUEST_SIZE=15MB

# Event Subscribers
SUBSCRIBERS_ENABLED=schedaSingolaVaccinazioneEsitoConsumer;jobCalcoloCoperturaVaccinaleEsitoConsumer;gestioneCentroErogazioneConsumer

# Anagrafe Centralizzata
ANAGRAFE_API_URL=http://localhost:8082
ANAGRAFE_API_TOKEN=your_token

# FSE Integrator
FSE_API_URL=http://localhost:8083
FSE_API_TOKEN=your_token

# AVN Integrator
AVN_API_URL=http://localhost:8084
AVN_API_TOKEN=your_token

# Logistica
LOGISTICA_API_URL=http://localhost:8085
LOGISTICA_API_TOKEN=your_token

# Business Rules Engine
BUSINESS_RULES_API_URL=http://localhost:8086

# Email Service
EMAIL_SERVICE_ENABLED=true
EMAIL_SERVICE_URL=http://localhost:8087

# Security
SICUREZZA_CENTRALIZZATA_URL=http://localhost:8088
JWT_SECRET=your_jwt_secret_key_here

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

### Profili Spring

Il progetto supporta diversi profili:

**`dev` (Development):**
- Test abilitati
- SQL logging attivo
- Connessioni a servizi locali

**`test` (Testing/Collaudo):**
- Connessioni a ambienti di test
- Cache disabilitata

**`prod` (Production):**
- Ottimizzazioni performance
- Logging minimale
- Connessioni produzione

**Attivazione profilo:**
```bash
# Via env variable
export SPRING_PROFILES_ACTIVE=dev

# Via command line
java -jar vaccinale-ms.jar --spring.profiles.active=dev

# In IDE: Run Configuration > Environment Variables
SPRING_PROFILES_ACTIVE=dev
```

### File di Configurazione

**Gerarchia:**
1. `src/main/resources/application.yaml` - Configurazione base
2. `src/main/resources/application-dev.yaml` - Override per dev
3. Environment variables - Override finale

**Esempio application-dev.yaml:**
```yaml
spring:
  jpa:
    show-sql: true

logging:
  level:
    com.dxc.sgisanita.vaccinale: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## Build

### Build Locale

**Build completo (con test):**
```bash
mvnw clean install
```

**Build senza test (più veloce):**
```bash
mvnw clean install -DskipTests
```

**Build parallelo (4 core):**
```bash
mvnw -T 4C clean install -DskipTests
```

**Output:**
- JAR: `target/vaccinale-ms-1.0.0-SNAPSHOT.jar`
- Dimensione: ~80-100 MB (fat JAR con tutte le dipendenze)

### Build con Profilo

**Profilo `fff` (Skip Tests - Default):**
```bash
mvnw clean install -Pfff
```

**Profilo `dev` (Run Tests):**
```bash
mvnw clean install -Pdev
```

### Compilazione MapStruct

MapStruct genera codice durante la compilazione:

```bash
# Forza rigenerazione mapper
mvnw clean compile
```

I mapper generati si trovano in:
```
target/generated-sources/annotations/com/dxc/sgisanita/vaccinale/mapper/
```

### Troubleshooting Build

**Errore "Cannot find symbol" su Mapper:**
```bash
# Pulisci e ricompila
mvnw clean compile
```

**Errore Lombok:**
```bash
# Verifica annotation processor nell'IDE
# IntelliJ: Settings > Build > Compiler > Annotation Processors > Enable
```

**Errore dipendenze Nexus:**
```bash
# Verifica connessione a Nexus
curl https://nexus.cdp-sanita-coll.soresa.it/repository/maven-public/

# Forza update dipendenze
mvnw clean install -U
```

---

## Esecuzione

### Run con Maven

```bash
mvnw spring-boot:run
```

**Con profilo:**
```bash
mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Con debug remoto:**
```bash
mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### Run da JAR

```bash
# Build
mvnw clean package -DskipTests

# Run
java -jar target/vaccinale-ms-1.0.0-SNAPSHOT.jar
```

**Con profilo e timezone:**
```bash
java -Dspring.profiles.active=dev \
     -Duser.timezone=Europe/Rome \
     -jar target/vaccinale-ms-1.0.0-SNAPSHOT.jar
```

**Con configurazione JVM:**
```bash
java -Xms512m -Xmx2048m \
     -XX:+UseG1GC \
     -Dspring.profiles.active=dev \
     -jar target/vaccinale-ms-1.0.0-SNAPSHOT.jar
```

### Run in IDE

#### IntelliJ IDEA

1. **Import progetto:**
   - File > Open > Seleziona `pom.xml`
   - Import as Maven project

2. **Abilita Annotation Processing:**
   - Settings > Build, Execution, Deployment > Compiler > Annotation Processors
   - ✅ Enable annotation processing

3. **Crea Run Configuration:**
   - Run > Edit Configurations > Add New > Spring Boot
   - Main class: `com.dxc.sgisanita.vaccinale.VaccinaleApplication`
   - Environment variables: Copia da `.env`
   - Active profiles: `dev`

4. **Run/Debug:** Usa i pulsanti run/debug nella toolbar

#### Eclipse

1. **Import progetto:**
   - File > Import > Existing Maven Projects
   - Seleziona directory `vaccinale-ms`

2. **Abilita Lombok:**
   - Help > Install New Software
   - Add: https://projectlombok.org/p2
   - Install

3. **Run as Spring Boot App:**
   - Right-click su `VaccinaleApplication.java`
   - Run As > Spring Boot App

### Verifica Avvio

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Risposta attesa:**
```json
{
  "status": "UP"
}
```

**Swagger UI:**
```
http://localhost:8080/swagger-ui.html
```

**API Docs JSON:**
```
http://localhost:8080/v3/api-docs
```

---

## Docker

### Build Immagine Docker

```bash
docker build -t vaccinale-ms:latest .
```

**Build process:**
1. Stage 1: Maven build (4 cores paralleli)
2. Stage 2: Copia JAR in OpenJDK 11 Alpine
3. Installa font per JasperReports (ttf-dejavu, MS Core Fonts)
4. Timezone: Europe/Rome

### Run Container

**Run singolo:**
```bash
docker run -d \
  --name vaccinale-ms \
  -p 8080:8080 \
  -e DATABASE_URL_VACC=jdbc:postgresql://host.docker.internal:5432/vaccinale_db \
  -e DATABASE_USERNAME=vacc_user \
  -e DATABASE_PASSWORD=your_password \
  -e RABBITMQ_HOST=host.docker.internal \
  -e RABBITMQ_PORT=5672 \
  -e RABBITMQ_USERNAME=admin \
  -e RABBITMQ_PASSWORD=admin \
  -e SPRING_PROFILES_ACTIVE=dev \
  vaccinale-ms:latest
```

**Note:** `host.docker.internal` permette al container di accedere a servizi sull'host (Windows/Mac).

### Docker Compose (Consigliato)

Crea `docker-compose.yml`:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: vaccinale_db
      POSTGRES_USER: vacc_user
      POSTGRES_PASSWORD: vacc_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-db.sql:/docker-entrypoint-initdb.d/init.sql

  rabbitmq:
    image: rabbitmq:3-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin
    ports:
      - "5672:5672"
      - "15672:15672"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  vaccinale-ms:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL_VACC: jdbc:postgresql://postgres:5432/vaccinale_db
      DATABASE_USERNAME: vacc_user
      DATABASE_PASSWORD: vacc_pass
      DATABASE_VACC_MAX_POOLSIZE: 10
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: admin
      RABBITMQ_PASSWORD: admin
      RABBITMQ_VH: /
      MINIO_URL: http://minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
      SPRING_PROFILES_ACTIVE: dev
      SHOW_SQL: "false"
    depends_on:
      - postgres
      - rabbitmq
      - minio
    restart: unless-stopped

volumes:
  postgres_data:
  minio_data:
```

**Start tutto:**
```bash
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f vaccinale-ms
```

**Stop tutto:**
```bash
docker-compose down
```

### Push su Registry

```bash
# Tag per registry
docker tag vaccinale-ms:latest <registry-url>/vaccinale-ms:1.0.0

# Login (se privato)
docker login <registry-url>

# Push
docker push <registry-url>/vaccinale-ms:1.0.0
```

---

## Troubleshooting

### Problemi Database

**Errore: "relation does not exist"**
```
Causa: Schema vacc20 o tabelle non create
Soluzione: Esegui script SQL migrations
```

**Errore: "Connection refused"**
```bash
# Verifica PostgreSQL in ascolto
psql -U postgres -h localhost -p 5432

# Verifica connection string
echo $DATABASE_URL_VACC
```

**Errore: "too many connections"**
```
Causa: Connection pool esaurito
Soluzione: Aumenta DATABASE_VACC_MAX_POOLSIZE o max_connections PostgreSQL
```

### Problemi RabbitMQ

**Errore: "Connection refused"**
```bash
# Verifica RabbitMQ running
docker ps | grep rabbitmq

# Verifica porta
telnet localhost 5672

# Check logs
docker logs rabbitmq
```

**Errore: "access to vhost '/' refused"**
```
Causa: Virtual host non esistente o permessi mancanti
Soluzione: Crea VH e imposta permessi (vedi Setup RabbitMQ)
```

### Problemi MinIO

**Errore: "The specified bucket does not exist"**
```bash
# Verifica bucket esistenti
mc ls local/

# Crea bucket mancante
mc mb local/consensi-bucket
```

**Errore: "Access Denied"**
```
Causa: Credenziali errate o policy bucket
Soluzione: Verifica MINIO_ACCESS_KEY e MINIO_SECRET_KEY
```

### Problemi Build

**Errore: "Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin"**
```bash
# Pulisci cache Maven
mvnw clean

# Verifica Java version
java -version  # Deve essere 11+
```

**Errore: "Cannot resolve dependencies"**
```bash
# Forza update da Nexus
mvnw clean install -U

# Verifica connessione Nexus
curl https://nexus.cdp-sanita-coll.soresa.it/repository/maven-public/
```

**Errore MapStruct: "Unmapped target properties"**
```
Causa: Mismatch tra Entity e DTO fields
Soluzione: Aggiorna @Mapping annotations o usa @Mapping(target = "field", ignore = true)
```

### Problemi Runtime

**Errore: "OutOfMemoryError: Java heap space"**
```bash
# Aumenta heap size
java -Xms1g -Xmx4g -jar vaccinale-ms.jar
```

**Errore: "Port 8080 already in use"**
```bash
# Trova processo
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# Kill processo
taskkill /PID <PID> /F        # Windows
kill -9 <PID>                 # Linux/Mac

# Oppure cambia porta
java -jar vaccinale-ms.jar --server.port=8081
```

**Errore JasperReports: "net.sf.jasperreports.engine.util.JRFontNotFoundException"**
```
Causa: Font mancanti per report PDF
Soluzione:
  - Docker: già inclusi nel Dockerfile
  - Locale: installa ttf-dejavu e MS Core Fonts
```

### Problemi Integrazioni

**Errore: "Connection timeout" su API esterne**
```
Causa: Servizio esterno non raggiungibile
Soluzione:
  1. Verifica URL e credenziali
  2. Test connessione: curl -v <EXTERNAL_API_URL>
  3. Controlla firewall/proxy
```

**Errore: "401 Unauthorized" su FSE/Anagrafe**
```
Causa: Token JWT scaduto o errato
Soluzione: Verifica <SERVICE>_API_TOKEN in env variables
```

### Debug Logging

**Abilita log dettagliato:**

Crea `src/main/resources/application-debug.yaml`:
```yaml
logging:
  level:
    root: INFO
    com.dxc.sgisanita.vaccinale: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.web: DEBUG
    org.springframework.amqp: DEBUG
    com.zaxxer.hikari: DEBUG
```

**Run con profilo debug:**
```bash
java -jar vaccinale-ms.jar --spring.profiles.active=debug
```

---

## Performance Tuning

### JVM Options (Production)

```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -Xloggc:gc.log \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/logs/heapdump.hprof \
     -Duser.timezone=Europe/Rome \
     -Dspring.profiles.active=prod \
     -jar vaccinale-ms.jar
```

### Database Connection Pool

**Calcolo optimal pool size:**
```
pool_size = (core_count * 2) + effective_spindle_count
```

Esempio: 4 cores + 1 disk = 9 connessioni

**In application.yaml:**
```yaml
spring:
  datasource:
    hikari:
      maximumPoolSize: 10
      minimumIdle: 5
      connectionTimeout: 30000
      idleTimeout: 600000
      maxLifetime: 1800000
```

### Hazelcast Cache

**Per deployment multi-istanza, configura cluster:**

```yaml
hazelcast:
  network:
    join:
      multicast:
        enabled: false
      tcp-ip:
        enabled: true
        members:
          - 192.168.1.10
          - 192.168.1.11
```

---

## Sicurezza

### Checklist Pre-Production

- [ ] Cambia credenziali di default (DB, RabbitMQ, MinIO)
- [ ] Usa HTTPS per tutte le comunicazioni
- [ ] JWT_SECRET deve essere complesso (32+ caratteri)
- [ ] Abilita SSL per PostgreSQL
- [ ] Configura firewall (solo porte necessarie aperte)
- [ ] Abilita audit logging
- [ ] Backup regolari database
- [ ] Monitoring e alerting attivi
- [ ] Log rotation configurato
- [ ] Secrets gestiti tramite vault (non in env files)

---

## Risorse

- [ARCHITECTURE.md](ARCHITECTURE.md) - Architettura sistema
- [API_REFERENCE.md](API_REFERENCE.md) - Documentazione API
- [DEVELOPMENT.md](DEVELOPMENT.md) - Guida sviluppo
- [INTEGRATIONS.md](INTEGRATIONS.md) - Integrazioni esterne

**Supporto:**
- Team: DXC Technology - Sanità SGI
- Nexus: https://nexus.cdp-sanita-coll.soresa.it
- Jira: PIAT project

**Last Updated:** 2026-01-20
