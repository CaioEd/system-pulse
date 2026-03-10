# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (auto-starts PostgreSQL via Docker Compose)
./mvnw spring-boot:run

# Build (compile + package)
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SystemPulseApplicationTests

# Compile only
./mvnw compile
```

**Prerequisites:** Java 21 and Docker must be installed. The application uses Spring Boot's Docker Compose integration to automatically start/stop PostgreSQL (`docker/docker-compose.db.yml`) when the app starts/stops.

## Architecture

**System Pulse** is a Spring Boot 4 backend that acts as a central hub for server health monitoring. It uses a push-based heartbeat model (servers call the API, not the other way around) to work through NAT/firewalls.

### Data Flow

```
Go Agent → POST /api/v1/servers/heartbeat (UUID token + CPU/RAM/Disk %)
              ↓
         ServerController → ServerTelemetryService
              ↓                      ↓
         PostgreSQL DB        NotificationService → WebSocket /topic/server-status
                                                  ← Next.js Dashboard subscribes
```

### Key Services

- **`ServerService`** — CRUD for server registration. Generates a UUID token on creation (immutable, used by agents to authenticate).
- **`ServerTelemetryService`** — Processes heartbeats, updates server metrics, and runs a `@Scheduled` task every 10s to mark servers OFFLINE if no heartbeat in the last 45s.
- **`NotificationService`** — Broadcasts `StatusUpdateEvent` records via STOMP WebSocket to `/topic/server-status`.

### WebSocket

- Endpoint: `/ws-pulse` (STOMP over SockJS)
- Subscribe topic: `/topic/server-status`
- Application prefix: `/app`
- CORS allowed for `http://localhost:3000`

### Key DTOs (Java Records)

- `TelemetryDTO` — inbound heartbeat payload (token, cpu%, ram%, disk%)
- `StatusUpdateEvent` — outbound WebSocket event
- `ServerAlertDTO` — alert notification (added in `feat/messaging` branch with AMQP support)

### Package Structure

```
com.remote.system_pulse/
├── config/          # AsyncConfig (virtual threads), SecurityConfig, WebSocketConfig
├── controller/      # ServerController (REST endpoints)
├── service/         # ServerService, ServerTelemetryService, NotificationService
├── model/           # Server (JPA entity), enums/ServerStatus
├── repository/      # ServerRepository (findServerByToken custom query)
├── dto/             # DTOs as Java Records
└── utils/           # IpRegex (IPv4/IPv6 validation patterns)
```

## Configuration

`src/main/resources/application.yml` — Spring datasource and Docker Compose lifecycle settings. DB credentials are hardcoded for local dev (`user`/`postgresdbpassword`).

## API Documentation

Swagger UI available at `http://localhost:8080/swagger-ui.html` when running. A Postman collection is at `postman/servers_spring.postman_collection.json`.

## Active Development

The current branch `feat/messaging` is adding RabbitMQ/AMQP support (`spring-boot-starter-amqp` dependency + `ServerAlertDTO`). This is in progress — the messaging infrastructure is not yet wired up.
