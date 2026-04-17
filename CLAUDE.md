# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application (Docker Compose for PostgreSQL starts automatically)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ServerServiceTest

# Run a single test method
./mvnw test -Dtest=ServerServiceTest#createServer_ShouldReturnDTO_WhenSuccessful

# Build (skip tests)
./mvnw package -DskipTests
```

## Architecture

System Pulse is a Spring Boot 3.2.2 / Java 21 backend for real-time server health monitoring.

**Request flow:**
1. Remote agents (Go) POST to `/api/v1/servers/heartbeat` with a UUID token + telemetry data (CPU/RAM/Disk).
2. `ServerTelemetryService.updateTelemetry()` authenticates via `token`, updates metrics and `lastHeartbeat`, then pushes a STOMP message to `/topic/status`.
3. A `@Scheduled` job in `ServerTelemetryService.checkOfflineServers()` runs every 10s, marks servers with no heartbeat in the last 60s as `OFFLINE`, and broadcasts the change.
4. Frontend (Next.js) subscribes to `/topic/status` via WebSocket endpoint `/ws-pulse` (SockJS fallback).

**Key services:**
- `ServerService` — CRUD for the server registry (name, description). Token is auto-generated via `@PrePersist` on the `Server` entity.
- `ServerTelemetryService` — Handles heartbeat ingestion and the offline-detection scheduler.
- `NotificationService` — Thin wrapper around `SimpMessagingTemplate` for ad-hoc status change events.

**Infrastructure:**
- Spring automatically starts/stops PostgreSQL via `docker/docker-compose.db.yml` (Spring Docker Compose integration, configured in `application.yml`).
- Schema is managed by Hibernate (`ddl-auto: update`).
- CORS is locked to `http://localhost:3000`. To allow other origins, update `SecurityConfig.corsConfigurationSource()`.
- All endpoints are currently open (no authentication). The `token` UUID on the `Server` entity is the only form of agent identity.

**API docs:** `http://localhost:8080/swagger-ui.html` (springdoc-openapi)

**WebSocket destinations:**
- Connect: `/ws-pulse`
- Subscribe (client): `/topic/status`
- Send to server (client → app): prefix `/app`
