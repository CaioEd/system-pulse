# Heartbeat flow hardening

Branch: `fix/token-security` · Commit: `d14eb55`

This document summarizes the changes made to the heartbeat ingestion path
(`POST /api/v1/servers/heartbeat` → DB persistence → WebSocket broadcast)
and explains the reasoning behind each one.

## TL;DR for agent operators (BREAKING)

The agent token now travels in the `X-Agent-Token` request header instead
of inside the JSON body.

```http
POST /api/v1/servers/heartbeat
X-Agent-Token: <uuid>
Content-Type: application/json

{ "usageCpu": 45.5, "usageRam": 60.2, "usageDisk": 30.0 }
```

Old request shape (`{"token": "...", "usageCpu": ...}`) is no longer accepted —
requests without the header receive `401 Unauthorized`.

## Heartbeat flow today

```
Agent (Go) ──HTTP POST /api/v1/servers/heartbeat──▶ ServerController
       header: X-Agent-Token: <uuid>
       body:   { usageCpu, usageRam, usageDisk }
                      │
                      ▼
        @Valid validates body (NotNull, 0..100)
                      │
                      ▼
   ServerTelemetryService.updateTelemetry  (@Transactional)
        │  findServerByToken(headerToken)
        │     ├─ missing/unknown → InvalidAgentTokenException → 401
        │     └─ found → mutate fields (no explicit save; dirty checking flushes on commit)
        ▼
   returns StatusUpdateEvent (token-free DTO)
                      │
                      ▼   (transaction has committed at this point)
   NotificationService.notifyStatusChange(event)
                      │
                      ▼
        SimpMessagingTemplate → /topic/status   ← subscribers see DTO, never the entity
```

The `@Scheduled` offline-detection job (every 10s) follows the same pattern:
it queries `ServerRepository.findStaleOnlineServers(...)` (filter pushed to
SQL), flips the status, and broadcasts a `StatusUpdateEvent` per stale server.

## What changed and why

### Security

| # | Change | Why |
|---|---|---|
| 1 | **WebSocket payload switched from `Server` entity to `StatusUpdateEvent`** | `messagingTemplate.convertAndSend("/topic/status", savedServer)` was serializing the full JPA entity, including the `token` UUID, to every STOMP subscriber. Anyone able to subscribe could harvest agent tokens and impersonate them. The DTO has no token field. |
| 2 | **Token moved to `X-Agent-Token` header** (was in JSON body) | Bodies are commonly captured by access logs, request-body inspectors, and APM tools. Headers are easier to redact and align with standard authentication conventions. |
| 3 | **`@Valid` added to `TelemetryDTO`** in the controller | The `@NotNull` / `@Min(0)` / `@Max(100)` constraints on the DTO were never enforced before — the controller was missing `@Valid`. Agents could send `usageCpu = 99999.0` or omit fields entirely. Also tightened the DTO to require all three metrics. |
| 4 | **Typed `InvalidAgentTokenException` mapped to 401** | Previously any token problem produced `RuntimeException("Server not found with provided token")` → `500 Internal Server Error`, which leaked an internal message and wrongly routed the failure to error dashboards/alerts. Now the API returns `401 Unauthorized` with a generic body. |
| 5 | **`GlobalExceptionHandler` for validation errors → 400** | `MethodArgumentNotValidException` is now handled into a structured `400 Bad Request` with a `fields` map, instead of bubbling up as a default Spring error page. |

### Performance

| # | Change | Why |
|---|---|---|
| 6 | **`ServerRepository.findStaleOnlineServers(...)` JPQL** | The scheduler used to do `serverRepository.findAll().stream().filter(...)`, transferring every server row from the DB every 10 seconds even when nothing was stale. The filter is now pushed to SQL via a `WHERE` clause. |
| 7 | **Removed redundant `serverRepository.save(server)`** inside `@Transactional` | Hibernate dirty-checking flushes the transaction's mutations on commit. The explicit `save` is unnecessary and forces an early flush, costing extra round-trips. |
| 8 | **WebSocket broadcast moved out of the heartbeat transaction** | Broadcasting inside the transactional service method meant a slow/blocked STOMP broker would keep the DB transaction open. The transactional service method now returns a `StatusUpdateEvent` and the controller broadcasts after the call returns (i.e., after commit). |

### Architecture

| # | Change | Why |
|---|---|---|
| 9 | **`NotificationService` is now actually used** | It already existed but was dead code — the heartbeat path went directly through `SimpMessagingTemplate`. Both broadcast call sites (heartbeat and scheduler) now go through this single service. |
| 10 | **Service no longer leaks the JPA entity** | The contract of `updateTelemetry(...)` returns `StatusUpdateEvent`, not `Server`. WebSocket payload is decoupled from the Hibernate-managed entity, which is both safer (see #1) and a cleaner boundary. |
| 11 | **New `exception/` package** with `InvalidAgentTokenException` and `GlobalExceptionHandler` | Establishes the place future typed exceptions and their HTTP mappings should live. The other `RuntimeException` call sites in `ServerService` / `UserService` can migrate here in follow-ups. |

## Files touched

```
src/main/java/com/remote/system_pulse/
  controller/ServerController.java               (modified)
  dto/TelemetryDTO.java                          (modified — token field removed)
  exception/InvalidAgentTokenException.java      (new)
  exception/GlobalExceptionHandler.java          (new)
  repository/ServerRepository.java               (modified — added JPQL query)
  service/ServerTelemetryService.java            (modified)

src/test/java/com/remote/system_pulse/
  service/ServerTelemetryServiceTest.java        (modified)

postman/servers_spring.postman_collection.json   (modified — heartbeat request + collection variable)
README.md                                        (modified — Postman usage section)
docs/heartbeat_hardening.md                      (this file)
```

## How to test

### Unit tests
```bash
./mvnw test -Dtest='!ApplicationTests'
```
Expected: `Tests run: 35, Failures: 0, Errors: 0`. The 5 `ServerTelemetryServiceTest`
cases cover: happy path, unknown token (`InvalidAgentTokenException`), missing
header (null token), stale server flipping to OFFLINE without token leak, and
the empty-stale-list scheduler path.

### Integration / context test
```bash
./mvnw test -Dtest=ApplicationTests
```
Requires a working Docker daemon — Spring Boot's Docker Compose integration
will bring up `docker/docker-compose.db.yml`.

### Manual smoke test
1. **Register a server** (need a logged-in user — see `POST /api/v1/auth/login`):
   ```bash
   curl -X POST http://localhost:8080/api/v1/servers \
     -H 'Content-Type: application/json' \
     --cookie 'jwt=<JWT-FROM-LOGIN>' \
     -d '{"name":"test-srv","description":"smoke"}'
   # response → { "id": ..., "token": "<UUID>", ... }
   ```
2. **Heartbeat — happy path** (200):
   ```bash
   curl -i -X POST http://localhost:8080/api/v1/servers/heartbeat \
     -H 'X-Agent-Token: <UUID>' \
     -H 'Content-Type: application/json' \
     -d '{"usageCpu":42.5,"usageRam":60.0,"usageDisk":75.3}'
   ```
3. **Bad token → 401**:
   ```bash
   curl -i -X POST http://localhost:8080/api/v1/servers/heartbeat \
     -H 'X-Agent-Token: 00000000-0000-0000-0000-000000000000' \
     -H 'Content-Type: application/json' \
     -d '{"usageCpu":1,"usageRam":1,"usageDisk":1}'
   ```
4. **Missing header → 400** (Spring rejects the missing required header):
   ```bash
   curl -i -X POST http://localhost:8080/api/v1/servers/heartbeat \
     -H 'Content-Type: application/json' \
     -d '{"usageCpu":1,"usageRam":1,"usageDisk":1}'
   ```
5. **Bad payload → 400 with `fields`**:
   ```bash
   curl -i -X POST http://localhost:8080/api/v1/servers/heartbeat \
     -H 'X-Agent-Token: <UUID>' \
     -H 'Content-Type: application/json' \
     -d '{"usageCpu":150}'
   ```
6. **Token leak check**: connect a STOMP client to `ws://localhost:8080/ws-pulse`,
   subscribe to `/topic/status`, send a heartbeat, and confirm the JSON received
   on the topic does **not** contain a `token` field.
7. **Offline detection**: register a server, send one heartbeat, wait > 60s.
   Within the next 10s the scheduler flips it to `OFFLINE` and pushes a
   `StatusUpdateEvent` with `status: "OFFLINE"`.

## Out of scope (intentional follow-ups)

* **Replay protection** (timestamp + nonce signing on heartbeats).
* **Rate limiting** on `/heartbeat` per token.
* **`@TransactionalEventListener(AFTER_COMMIT)`** for the scheduler's broadcast — the heartbeat path already broadcasts post-commit; the scheduler still broadcasts inside its transaction, which is no worse than before.
* **Migrating remaining `RuntimeException` call sites** in `ServerService` / `UserService` to typed exceptions handled by `GlobalExceptionHandler`.
