# Support System

Микросервисная система автоматизации технической поддержки на базе Quarkus.

## Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                     Client / Swagger UI                      │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST + JWT (Keycloak)
                           ▼
                   ┌───────────────┐
                   │incident-service│  :8081
                   └───────┬───────┘
                           │ gRPC         gRPC
              ┌────────────┼─────────────────────┐
              ▼            │                     ▼
      ┌──────────────┐     │ Kafka        ┌──────────────┐
      │ user-service │     │ incident-    │ image-service│
      │ :8082        │     │ events       │ :8083        │
      │ gRPC: 9007   │     │              │ gRPC: 9008   │
      └──────────────┘     ▼              └──────┬───────┘
              ▲   ┌──────────────────┐           │
              │   │notification-svc  │           │ MinIO
              └───│ :8084            │           ▼
               gRPC└──────────────────┘    ┌──────────┐
                                           │  MinIO   │
                                           │  :9000   │
                                           └──────────┘
```

### Сервисы

| Сервис               | Порт  | gRPC  | Описание                                  |
|----------------------|-------|-------|-------------------------------------------|
| incident-service     | 8081  | —     | Управление инцидентами, REST API          |
| user-service         | 8082  | 9007  | Управление пользователями + Keycloak      |
| image-service        | 8083  | 9008  | Хранение изображений в MinIO              |
| notification-service | 8084  | —     | Email-уведомления через Kafka             |

### Инфраструктура

| Сервис        | Порт        | Описание                    |
|---------------|-------------|-----------------------------|
| Keycloak      | 8080        | OAuth2 / OIDC авторизация   |
| PostgreSQL    | 5432–5434   | БД для каждого сервиса      |
| Kafka         | 29092       | Брокер сообщений            |
| MinIO         | 9000 / 9001 | S3-совместимое хранилище    |

---

## Быстрый старт

### Требования

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### Запуск

```bash
# 1. Сборка всех сервисов
mvn clean install -DskipTests

# 2. Запуск всего стека одной командой
docker-compose up -d --build
```

Docker Compose автоматически запустит инфраструктуру в правильном порядке:
1. PostgreSQL (все 4 инстанса)
2. Keycloak, Kafka, MinIO
3. Микросервисы

Полная готовность стека — ~2 минуты (Keycloak стартует дольше всех).

### Проверка статуса

```bash
docker-compose ps
```

Все сервисы должны быть в статусе `Up`.

---

## Авторизация

Realm `support` импортируется автоматически из `keycloak/realm-export.json` при первом запуске.

### Тестовый пользователь

| Поле     | Значение         |
|----------|------------------|
| Username | testuser         |
| Password | 123              |
| Email    | test@test.com    |
| Роль     | admin            |

### Получение токена

```bash
curl -X POST http://localhost:8080/realms/support/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=incident-service" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=testuser" \
  -d "password=123"
```

Секреты клиентов задаются в файле `.env` в корне проекта:

```env
USER_SERVICE_OIDC_SECRET=your_secret
INCIDENT_SERVICE_OIDC_SECRET=your_secret
```

### Swagger UI

Все запросы через Swagger UI требуют Bearer токен:

| Сервис           | Swagger UI                           |
|------------------|--------------------------------------|
| incident-service | http://localhost:8081/swagger-ui     |
| user-service     | http://localhost:8082/swagger-ui     |

### Первый запуск: создание пользователя в системе

Keycloak хранит учётные данные для авторизации, но для работы с API также нужна запись пользователя в БД `user-service`. При создании инцидента сервис ищет пользователя по `keycloakId` (поле `sub` из JWT токена).

**Порядок действий:**

1. Получите токен `testuser` (см. выше)
2. Создайте пользователя в системе через `POST /api/v1/users` в Swagger UI `user-service`:

```json
{
  "role": "ADMIN",
  "userFullName": "Ivan Petrov",
  "userLogin": "ivan.petrov",
  "phoneNumber": "+79001234567",
  "email": "ivan.petrov@company.com",
  "workplace": "Office 301",
  "password": "SecurePass123!"
}
```
3. Получите токен `ivan.petrov` (см. выше)
4. После этого все операции с инцидентами будут работать корректно — сервис найдёт пользователя по `keycloakId` и привяжет его как инициатора.

---

## API

### Incidents (`/api/v1/incidents`)

| Метод  | Путь                                      | Роли                    | Описание                  |
|--------|-------------------------------------------|-------------------------|---------------------------|
| GET    | /api/v1/incidents                         | admin, analyst, user    | Список инцидентов         |
| GET    | /api/v1/incidents/{id}                    | admin, analyst, user    | Инцидент по ID            |
| POST   | /api/v1/incidents                         | admin, analyst, user    | Создать инцидент          |
| PUT    | /api/v1/incidents/{id}                    | admin, analyst, user    | Обновить инцидент         |
| DELETE | /api/v1/incidents/{id}                    | admin, analyst          | Удалить инцидент          |
| PATCH  | /api/v1/incidents/{id}/status             | admin, analyst          | Изменить статус           |
| PATCH  | /api/v1/incidents/{id}/priority           | admin, analyst          | Изменить приоритет        |
| PATCH  | /api/v1/incidents/{id}/category           | admin, analyst          | Изменить категорию        |
| PATCH  | /api/v1/incidents/{id}/analyst            | admin, analyst          | Назначить аналитика       |
| PATCH  | /api/v1/incidents/{id}/responsible-service| admin, analyst          | Назначить ответственный сервис |
| POST   | /api/v1/incidents/{id}/images             | admin, analyst, user    | Загрузить изображение     |
| GET    | /api/v1/incidents/{id}/images/{imgId}/download | admin, analyst, user | Скачать изображение  |
| DELETE | /api/v1/incidents/{id}/images/{imgId}     | admin, analyst          | Удалить изображение       |

### Users (`/api/v1/users`)

| Метод  | Путь                    | Роли   | Описание              |
|--------|-------------------------|--------|-----------------------|
| GET    | /api/v1/users           | admin  | Список пользователей  |
| GET    | /api/v1/users/{id}      | admin  | Пользователь по ID    |
| POST   | /api/v1/users           | admin  | Создать пользователя  |
| PUT    | /api/v1/users/{id}      | admin  | Обновить пользователя |
| DELETE | /api/v1/users/{id}      | admin  | Удалить пользователя  |

---

## Kafka

Один топик `incident-events` для всех событий:

| Событие            | Producer         | Consumer             | Действие                          |
|--------------------|------------------|----------------------|-----------------------------------|
| INCIDENT_CREATED   | incident-service | notification-service | Email всем пользователям с ролью admin |
| INCIDENT_DELETED   | incident-service | image-service        | Удаление всех изображений инцидента |

---

## Локальная разработка

```bash
# Только инфраструктура
docker-compose up -d postgres-incident postgres-user postgres-image \
  postgres-keycloak keycloak kafka zookeeper minio

# Запуск сервиса в dev-режиме
cd incident-service
mvn quarkus:dev
```

---

## Тесты

```bash
# Все тесты
mvn test

# Конкретный сервис
cd user-service && mvn test
```

---

## Структура проекта

```
support-system/
├── common-proto/           # .proto файлы + сгенерированные gRPC stubs
│   └── src/main/proto/
│       ├── user_service.proto
│       └── image_service.proto
├── incident-service/       # REST API, Kafka producer, gRPC client
├── user-service/           # gRPC сервер :9007, Keycloak Admin API
├── image-service/          # gRPC сервер :9008, MinIO, Kafka consumer
├── notification-service/   # Kafka consumer, email (mock по умолчанию)
├── keycloak/
│   └── realm-export.json   # Автоимпорт realm при первом запуске
├── docker-compose.yml
├── .env                    # Секреты (не коммитить!)
└── pom.xml
```