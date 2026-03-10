# Support System — Deployment Guide

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client / Swagger UI                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ REST (JWT)
                             ▼
                      incident-service
                           :8081
                        ┌────┴────┐
                   gRPC │         │ gRPC
                        ▼         ▼
                  user-service  image-service
                     :8082         :8083
                             │
                        Kafka │ incident-events
                    ┌─────────┴──────────┐
                    ▼                    ▼
           notification-service    image-service
                 :8084               :8083
                    │
               gRPC │ (get admins)
                    ▼
              user-service
                 :8082
```

> **Note:** All services use `quarkus.grpc.server.use-separate-server=false` —
> gRPC runs on the same port as HTTP, no separate gRPC port.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

## Quick Start (Docker)

```bash
# 1. Clone the repository
git clone https://gitlab.com/your-username/support-system.git
cd support-system

# 2. Build all services
mvn clean package -DskipTests

# 3. Start the full stack
docker-compose up -d

# 4. Wait ~30 seconds for Keycloak to initialize, then check:
docker-compose ps
```

### Service URLs

| Service              | URL                              |
|----------------------|----------------------------------|
| incident-service     | http://localhost:8081/swagger-ui |
| user-service         | http://localhost:8082/swagger-ui |
| image-service        | http://localhost:8083            |
| notification-service | http://localhost:8084            |
| Keycloak Admin       | http://localhost:8080            |
| MinIO Console        | http://localhost:9001            |

## Keycloak Setup

1. Open http://localhost:8080 → admin / admin
2. Create realm: `support`
3. Create clients:
    - `incident-service` — standard client
    - `user-service` — enable **Service accounts**, assign `realm-admin` role to service account
4. Create realm roles: `admin`, `analyst`, `user`
5. Create test users, assign roles, set **Email Verified = ON**, **Temporary password = OFF**

Token lifetime recommended: **1 hour**

### Getting a token

```bash
curl -X POST http://localhost:8080/realms/support/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=incident-service" \
  -d "client_secret=<secret>" \
  -d "username=<user>" \
  -d "password=<password>"
```

## Local Development (IntelliJ)

```bash
# 1. Start infrastructure
docker-compose up -d postgres-incident postgres-user postgres-image keycloak kafka minio zookeeper

# 2. Build common-proto first (required before running any service)
mvn install -pl common-proto

# 3. Run each service via IntelliJ:
#    Maven → <service> → Plugins → quarkus → quarkus:dev
#
#    Or create a Compound Run Configuration to start all at once:
#    Run → Edit Configurations → + → Maven (one per service, command: quarkus:dev)
#    Run → Edit Configurations → + → Compound → add all Maven configs
```

## Running Tests

```bash
# All tests
mvn test

# Single service
cd incident-service && mvn test
```

## Kafka Topics

| Topic             | Producer         | Consumers                                  | Events                                      |
|-------------------|------------------|--------------------------------------------|---------------------------------------------|
| incident-events   | incident-service | notification-service, image-service        | `INCIDENT_CREATED`, `INCIDENT_DELETED`      |

Each consumer has its own `group.id` (`notification-service` / `image-service`), so both receive every message independently.

`INCIDENT_CREATED`:
```json
{ "eventType": "INCIDENT_CREATED", "incidentId": 1, "incidentName": "Server down" }
```

`INCIDENT_DELETED`:
```json
{ "eventType": "INCIDENT_DELETED", "incidentId": 1 }
```

## Environment Variables

### incident-service

| Variable                | Default                  | Description                    |
|-------------------------|--------------------------|--------------------------------|
| DB_HOST                 | localhost                | PostgreSQL host                |
| DB_PORT                 | 5432                     | PostgreSQL port                |
| DB_NAME                 | incident_db              | Database name                  |
| DB_USER                 | postgres                 | Database user                  |
| DB_PASSWORD             | postgres                 | Database password              |
| KEYCLOAK_URL            | http://localhost:8080    | Keycloak base URL              |
| KEYCLOAK_REALM          | support                  | Keycloak realm                 |
| OIDC_SECRET             | —                        | incident-service client secret |
| KAFKA_BOOTSTRAP         | localhost:29092          | Kafka bootstrap servers        |
| USER_SERVICE_HOST       | localhost                | user-service host              |
| USER_SERVICE_GRPC_PORT  | 8082                     | user-service port (HTTP=gRPC)  |
| IMAGE_SERVICE_HOST      | localhost                | image-service host             |
| IMAGE_SERVICE_GRPC_PORT | 8083                     | image-service port (HTTP=gRPC) |

### user-service

| Variable                    | Default               | Description                       |
|-----------------------------|-----------------------|-----------------------------------|
| DB_HOST                     | localhost             | PostgreSQL host                   |
| DB_PORT                     | 5433                  | PostgreSQL port                   |
| DB_NAME                     | user_db               | Database name                     |
| DB_USER                     | postgres              | Database user                     |
| DB_PASSWORD                 | postgres              | Database password                 |
| KEYCLOAK_URL                | http://localhost:8080 | Keycloak base URL                 |
| KEYCLOAK_REALM              | support               | Keycloak realm                    |
| OIDC_SECRET                 | —                     | user-service client secret        |
| KEYCLOAK_ADMIN_CLIENT_ID    | user-service          | Client for Keycloak Admin API     |
| KEYCLOAK_ADMIN_CLIENT_SECRET| —                     | Secret for Keycloak Admin API     |

### image-service

| Variable          | Default               | Description             |
|-------------------|-----------------------|-------------------------|
| DB_HOST           | localhost             | PostgreSQL host         |
| DB_PORT           | 5434                  | PostgreSQL port         |
| DB_NAME           | image_db              | Database name           |
| DB_USER           | postgres              | Database user           |
| DB_PASSWORD       | postgres              | Database password       |
| KAFKA_BOOTSTRAP   | localhost:29092       | Kafka bootstrap servers |
| MINIO_URL         | http://localhost:9000 | MinIO endpoint          |
| MINIO_ACCESS_KEY  | minioadmin            | MinIO access key        |
| MINIO_SECRET_KEY  | minioadmin            | MinIO secret key        |
| MINIO_BUCKET      | incident-images       | MinIO bucket name       |

### notification-service

| Variable              | Default               | Description                    |
|-----------------------|-----------------------|--------------------------------|
| KAFKA_BOOTSTRAP       | localhost:29092       | Kafka bootstrap servers        |
| USER_SERVICE_HOST     | localhost             | user-service host              |
| USER_SERVICE_GRPC_PORT| 8082                  | user-service port (HTTP=gRPC)  |
| MAIL_HOST             | smtp.gmail.com        | SMTP host                      |
| MAIL_PORT             | 587                   | SMTP port                      |
| MAIL_USERNAME         | noreply@company.com   | SMTP username                  |
| MAIL_PASSWORD         | —                     | SMTP password                  |
| MAIL_FROM             | noreply@company.com   | Sender address                 |

## API Overview

### incident-service `:8081`

| Method | Path                                        | Role                    | Description                  |
|--------|---------------------------------------------|-------------------------|------------------------------|
| GET    | /api/v1/incidents                           | admin, analyst, user    | Get all incidents            |
| GET    | /api/v1/incidents/{id}                      | admin, analyst, user    | Get incident with full details (users + images) |
| POST   | /api/v1/incidents                           | admin, analyst, user    | Create incident              |
| PUT    | /api/v1/incidents/{id}                      | admin, analyst, user    | Update incident              |
| DELETE | /api/v1/incidents/{id}                      | admin, analyst          | Delete incident              |
| PATCH  | /api/v1/incidents/{id}/status               | admin, analyst          | Change status                |
| PATCH  | /api/v1/incidents/{id}/priority             | admin, analyst          | Set priority                 |
| PATCH  | /api/v1/incidents/{id}/category             | admin, analyst          | Set category                 |
| PATCH  | /api/v1/incidents/{id}/analyst              | admin, analyst          | Assign analyst               |
| PATCH  | /api/v1/incidents/{id}/responsible-service  | admin, analyst          | Set responsible service      |
| POST   | /api/v1/incidents/{id}/images               | admin, analyst, user    | Upload image (multipart)     |
| GET    | /api/v1/incidents/{id}/images/{imageId}/download | admin, analyst, user | Download image          |
| DELETE | /api/v1/incidents/{id}/images/{imageId}     | admin, analyst          | Delete image                 |

### user-service `:8082`

| Method | Path                    | Role           | Description        |
|--------|-------------------------|----------------|--------------------|
| GET    | /api/v1/users           | admin          | Get all users      |
| GET    | /api/v1/users/{id}      | admin, analyst | Get user by ID     |
| GET    | /api/v1/users/role/{role} | admin, analyst | Get users by role |
| POST   | /api/v1/users           | admin          | Create user (also creates in Keycloak) |
| PUT    | /api/v1/users/{id}      | admin          | Update user        |
| DELETE | /api/v1/users/{id}      | admin          | Delete user        |

## Project Structure

```
support-system/
├── common-proto/           # Shared gRPC .proto files + generated stubs
│   └── src/main/proto/
│       ├── user_service.proto
│       └── image_service.proto
├── incident-service/       # Core incident management (port 8081)
├── user-service/           # User management + Keycloak integration (port 8082)
├── image-service/          # Image storage via MinIO (port 8083)
├── notification-service/   # Email notifications via Kafka (port 8084)
├── docker-compose.yml
└── pom.xml                 # Parent POM
```

## gRPC Services

| Service       | Port | Methods                                                       |
|---------------|------|---------------------------------------------------------------|
| user-service  | 8082 | GetUserById, GetUsersByRole, GetUserByKeycloakId              |
| image-service | 8083 | UploadImage, GetImagesByIncidentId, DeleteImage, DownloadImage|

> gRPC and HTTP share the same port (`use-separate-server=false`).