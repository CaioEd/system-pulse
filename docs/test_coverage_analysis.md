# System Pulse — Test Coverage Analysis

> Last updated: 2026-05-09 — branch `feature/unit-tests`

## Current coverage map (manual — performed by Claude Opus)

## Summary

| Metric | Value |
|---|---|
| **Total source files** | 28 |
| **Total testable classes** | 16 (services, controllers, configs, filter, model, utils) |
| **Classes with tests** | 3 (`ServerService`, `UserService`, `ServerTelemetryService`) |
| **Test files** | 4 (incl. the `ApplicationTests` smoke test) |
| **Total `@Test` methods** | 26 (8 + 12 + 5 + 1 context-load) |
| **Estimated class-level coverage** | **~19%** (3 / 16 testable classes) |
| **Estimated method-level coverage** | **~40%** (≈14 of ~35 public methods on testable classes) |

---

## What IS Currently Tested

### ✅ [ServerServiceTest](file:///home/caioe/Documentos/Pasta-Caio/sp/system-pulse/src/test/java/com/remote/system_pulse/service/ServerServiceTest.java) — 8 tests

| # | Test | Method covered |
|---|------|----------------|
| 1 | `createServer_ShouldReturnDTO_WhenSuccessful` | `createServer()` |
| 2 | `getServerById_ShouldReturnDTO_WhenFound` | `getServerById()` — happy path |
| 3 | `getServerById_ShouldThrowException_WhenNotFound` | `getServerById()` — error path |
| 4 | `getAllServers_ShouldReturnList` | `getAllServers()` |
| 5 | `updateServer_ShouldUpdateAndReturnDTO` | `updateServer()` — happy path |
| 6 | `updateServer_ShouldThrowException_WhenNotFound` | `updateServer()` — error path |
| 7 | `deleteServer_ShouldCallDelete_WhenFound` | `deleteServer()` — happy path |
| 8 | `deleteServer_ShouldThrowException_WhenNotFound` | `deleteServer()` — error path |

**Status:** all 5 public methods covered with both happy and error paths. ArgumentCaptor is used to assert the entity is mapped correctly before persistence.

---

### ✅ [UserServiceTest](file:///home/caioe/Documentos/Pasta-Caio/sp/system-pulse/src/test/java/com/remote/system_pulse/service/UserServiceTest.java) — 12 tests

| # | Test | Method covered |
|---|------|----------------|
| 1 | `createUser_ShouldReturnDTO_WhenSuccessful` | `createUser()` |
| 2 | `createUser_ShouldEncodePassword` | `createUser()` — verifies `PasswordEncoder` is invoked |
| 3 | `getAllUsers_ShouldReturnList` | `getAllUsers()` — single result |
| 4 | `getAllUsers_ShouldReturnEmptyList_WhenNoUserIsFound` | `getAllUsers()` — empty list |
| 5 | `getAllUsers_ShouldReturnMultipleUsers` | `getAllUsers()` — many results |
| 6 | `getUserById_ShouldReturnDTO_WhenFound` | `getUserById()` — happy path |
| 7 | `getUserById_ShouldThrowException_WhenNotFound` | `getUserById()` — error path |
| 8 | `updateUser_ShouldReturnUpdatedDTO_WhenFound` | `updateUser()` — happy path |
| 9 | `updateUser_ShouldSetAllFieldsOnExistingUser` | `updateUser()` — verifies field mutation via ArgumentCaptor |
| 10 | `updateUser_ShouldThrowException_WhenNotFound` | `updateUser()` — error path |
| 11 | `deleteUser_ShouldDeleteUser_WhenFound` | `deleteUser()` — happy path |
| 12 | `deleteUser_ShouldThrowException_WhenNotFound` | `deleteUser()` — error path |

**Status:** strong coverage. All 5 public methods exercised on happy + error paths, password-encoding side effect explicitly verified, and the update path checks that the existing entity is mutated rather than replaced.

**Remaining gap:**
- Duplicate-email rejection — currently **not enforced at the service layer**. If/when that constraint is added (or surfaced from a `DataIntegrityViolationException`), add a test for it.

---

### ✅ [ServerTelemetryServiceTest](file:///home/caioe/Documentos/Pasta-Caio/sp/system-pulse/src/test/java/com/remote/system_pulse/service/ServerTelemetryServiceTest.java) — 5 tests *(NEW)*

| # | Test | Method covered |
|---|------|----------------|
| 1 | `updateTelemetry_ShouldUpdateServer_WhenServerExists` | `updateTelemetry()` — happy path. Asserts CPU/RAM/Disk persisted, status flipped to `ONLINE`, IP captured from `HttpServletRequest`, `lastHeartbeat` set, WS broadcast on `/topic/status`. |
| 2 | `updateTelemetry_ShouldThrowException_WhenTokenNotFound` | `updateTelemetry()` — error path. Verifies no save, no broadcast. |
| 3 | `checkOfflineServers_ShouldMarkAsOffline_WhenHeartbeatIsOld` | `checkOfflineServers()` — stale server flipped to `OFFLINE` and broadcast; recent server untouched. |
| 4 | `checkOfflineServers_ShouldIgnoreRecentServers` | `checkOfflineServers()` — no-op when all servers are within the 60s threshold. |
| 5 | `checkOfflineServers_ShouldMarkOffline_WhenHeartbeatNull` | `checkOfflineServers()` — `null` heartbeat treated as stale. |

**Status:** the core heartbeat engine is now under test. Good use of `ArgumentCaptor` and explicit `verify(..., never())` checks on the negative paths.

---

### ✅ [ApplicationTests](file:///home/caioe/Documentos/Pasta-Caio/sp/system-pulse/src/test/java/com/remote/system_pulse/ApplicationTests.java) — 1 test
- `contextLoads()` — smoke test that the Spring context starts. Requires a running PostgreSQL (Spring Docker Compose integration), so it may fail in CI without Docker available.

---

## What is NOT Tested (by priority)

### 🔴 High Priority — Authentication backbone

#### 1. `JwtService` — **0 tests**

Without tests here, an accidental change to claim names, signing key handling, or expiration math will silently break every authenticated request.

| Method | Test suggestions |
|--------|------------------|
| `generate(User)` | • Returns a non-empty JWT.<br>• Subject = `user.getEmail()`.<br>• `role` claim = `user.getRole().name()`.<br>• `name` claim = `user.getName()`.<br>• `expiration` ≈ now + `expirationMs` (use `@Value` injection or reflection / `ReflectionTestUtils` to set the secret + expiration on the unit). |
| `extractUserEmail(String)` | • Returns the subject for a token produced by `generate()`.<br>• Throws `JwtException` for a tampered signature. |
| `isValid(String)` | • `true` for a freshly generated token.<br>• `false` for an expired token (build a token with a short `expirationMs` and sleep, **or** craft the token directly with `Jwts.builder().setExpiration(past)`).<br>• `false` for a tampered token.<br>• `false` for `null` / empty string (`IllegalArgumentException` branch). |

> [!TIP]
> For unit tests, inject the `secret` via `ReflectionTestUtils.setField(jwtService, "secret", base64Secret)` and a known `expirationMs`. The secret must be Base64-encoded and ≥ 256 bits for HS256.

#### 2. `CustomUserDetailsService` — **0 tests**

| Method | Test suggestions |
|--------|------------------|
| `loadUserByUsername(String email)` | • Valid email → returns a `UserDetails` whose username = email, password = stored hash, and authorities contain the user's role (`ROLE_OPERATOR` / `ROLE_ADMIN`, since `.roles(...)` prefixes with `ROLE_`).<br>• Unknown email → throws `UsernameNotFoundException`. |

---

### 🟡 Medium Priority — Controller layer

> [!IMPORTANT]
> There are still **zero controller tests**. Controller tests (`@WebMvcTest`) validate HTTP status codes, request validation (`@Valid`), JSON (de)serialisation, and security rules — all of which are invisible to service-level unit tests.

#### 3. `ServerController` — **0 tests** (6 endpoints)

| Endpoint | Test suggestions |
|----------|------------------|
| `POST /api/v1/servers` | • 201 Created with valid body.<br>• 400 Bad Request when `name` is blank (the entity has `@NotBlank` but the DTO does not — this test will document/expose that gap).<br>• 401 Unauthorized without JWT cookie. |
| `GET /api/v1/servers` | • 200 OK returns the list.<br>• 401 without auth. |
| `GET /api/v1/servers/{id}` | • 200 OK when found.<br>• 500 when service throws (no `@ControllerAdvice` exists yet — also a gap to flag). |
| `PUT /api/v1/servers/{id}` | • 200 OK on update.<br>• 400 on invalid body. |
| `DELETE /api/v1/servers/{id}` | • 204 No Content on success. |
| `POST /api/v1/servers/heartbeat` | • 200 OK without authentication (allow-listed in `SecurityConfig`).<br>• Bad/unknown token → 500 (or whatever the global handler turns it into). |

#### 4. `UsersController` — **0 tests** (5 endpoints)

Same `@WebMvcTest` pattern as `ServerController`. `POST /api/v1/users` is allow-listed for registration; the other four require authentication.

#### 5. `AuthController` — **0 tests** (2 endpoints)

| Endpoint | Test suggestions |
|----------|------------------|
| `POST /api/v1/auth/login` | • Valid credentials → 200 + `Set-Cookie: jwt=…; HttpOnly; Secure; Path=/; Max-Age=86400; SameSite=Strict`.<br>• `AuthenticationManager.authenticate` throws → 401 / `BadCredentialsException`.<br>• Authenticated but missing `User` row → `RuntimeException("User not found")` (currently 500 — also flag for a global handler). |
| `POST /api/v1/auth/logout` | • 200 OK + cookie cleared (`Max-Age=0`, empty value). |

---

### 🟠 Medium-Low Priority — Security / config

#### 6. `JwtCookieFilter` — **0 tests**

| Scenario | Expected behaviour |
|----------|-------------------|
| Valid JWT cookie | Authentication is set in `SecurityContextHolder`, principal is the `UserDetails` for that email. |
| No cookies on the request | Filter passes through; `SecurityContextHolder` is not modified. |
| Cookies present but no `jwt` cookie | Same as above. |
| `jwt` cookie with invalid/expired token | Filter passes through; no auth is set. |

Use a real `OncePerRequestFilter` invocation with `MockHttpServletRequest`/`MockHttpServletResponse` and a `MockFilterChain`; mock `JwtService` and `CustomUserDetailsService`.

#### 7. `SecurityConfig` — best covered via `@WebMvcTest`

Verify, against a `MockMvc` configured with the real filter chain:
- `/api/v1/auth/**` is public.
- `/api/v1/servers/heartbeat` is public.
- `POST /api/v1/users` is public (registration).
- All other endpoints require authentication (401 without cookie).
- CORS allows `http://localhost:3000` (preflight `OPTIONS` returns the right `Access-Control-Allow-Origin`).

---

### 🟢 Low Priority — Utility / model / config

#### 8. `NotificationService` — **0 tests**
- Single method, thin wrapper. One test: verify `messagingTemplate.convertAndSend("/topic/status", event)` is called with the event passed in.

#### 9. `IpRegex` — **0 tests**
- Parametric test on `IP_PATTERN` against valid IPv4 (`192.168.0.1`, `0.0.0.0`, `255.255.255.255`) and IPv6 (`fe80:0:0:0:0:0:0:1`) addresses, plus invalid samples (`999.1.1.1`, `not-an-ip`).
- Reflection test: invoking the private constructor throws `UnsupportedOperationException`.

#### 10. `Server.generateToken()` (`@PrePersist`) — **0 tests**
- Token is generated when `null` (unit test of the `generateToken()` method directly — no JPA needed).
- Token is **not** overwritten when already set.

#### 11. `AsyncConfig`, `WebSocketConfig` — low ROI for unit tests
- Better covered by an integration test that opens a SockJS/STOMP connection on `/ws-pulse` and subscribes to `/topic/status`. Skip until the controller/integration suite exists.

---

## Coverage Heatmap

```
CLASS                          TESTS  TESTED?  PRIORITY    NOTES
─────────────────────────────────────────────────────────────────────────────────
ServerService                    8      ✅      —           Full happy + error paths
UserService                     12      ✅      —           Full happy + error paths + side effects
ServerTelemetryService           5      ✅      —           updateTelemetry + checkOfflineServers
JwtService                       0      ❌      🔴 HIGH     Auth token generation/validation
CustomUserDetailsService         0      ❌      🔴 HIGH     User auth loading
ServerController                 0      ❌      🟡 MED      HTTP layer + validation (6 endpoints)
UsersController                  0      ❌      🟡 MED      HTTP layer + validation (5 endpoints)
AuthController                   0      ❌      🟡 MED      Login/logout + cookie attributes
JwtCookieFilter                  0      ❌      🟠 MED-LOW  Security filter chain
SecurityConfig                   —      ❌      🟠 MED-LOW  Tested transitively via @WebMvcTest
NotificationService              0      ❌      🟢 LOW      Thin wrapper (1 method)
IpRegex                          0      ❌      🟢 LOW      Regex utility
Server (model)                   0      ❌      🟢 LOW      @PrePersist token generation
AsyncConfig / WebSocketConfig    —      ❌      🟢 LOW      Better via integration tests
```

---

## Recommended Action Plan

### Phase 1 — Auth backbone (small, high-leverage)
1. **`JwtServiceTest`** — 5–6 tests for `generate`, `extractUserEmail`, `isValid` (incl. expired/tampered/null branches).
2. **`CustomUserDetailsServiceTest`** — 2 tests (found / not found).

> Estimated bump: ~40% → ~55% method-level coverage. Closes the highest-risk gap (broken auth = silently broken every request).

### Phase 2 — Controller integration tests (`@WebMvcTest`)
3. **`AuthControllerTest`** — 4–5 tests, including cookie-attribute assertions (HttpOnly, Secure, SameSite=Strict, Max-Age).
4. **`ServerControllerTest`** — 8–10 tests across the 6 endpoints, including `/heartbeat` allow-list.
5. **`UsersControllerTest`** — 6–8 tests across the 5 endpoints, including `POST /users` allow-list.

> Phase 2 also exercises `SecurityConfig` rules and `@Valid` request validation. Expect ~75% method-level coverage after this phase.

### Phase 3 — Security filter, utilities, edge cases
6. **`JwtCookieFilterTest`** — 4 tests (valid / no cookie / wrong cookie / invalid token).
7. **`NotificationServiceTest`** — 1 test.
8. **`IpRegexTest`** — 4 tests (valid IPv4, valid IPv6, invalid, private-constructor guard).
9. **`Server#generateToken`** — 2 tests (sets when null, preserves when set).

> Estimated final bump: **~85–90% method-level coverage**.

---

## Side notes / follow-ups uncovered during analysis

These aren't test gaps per se, but worth flagging:

- **No global exception handler.** `RuntimeException("Server not found …")` and friends propagate as 500. A `@RestControllerAdvice` mapping these to 404 would (a) make controller tests cleaner and (b) stop leaking stack traces to clients.
- **`ServerRequestDTO` and `UserRequestDTO` lack bean-validation annotations.** `@Valid` in the controllers is a no-op without `@NotBlank` / `@Email` / `@Size` on the DTO records. The 400-on-blank-name controller test will only pass after that is added.
- **`AuthController.login` calls `userRepository.findByEmail` after `authManager.authenticate` already loaded the user via `CustomUserDetailsService`.** That is a redundant DB hit. Worth refactoring (and would shrink the test surface).
- **`AuthService.java` deletion is uncommitted on this branch.** Commit it cleanly before opening the PR so reviewers don't have to reconcile a stale class with the new auth flow.
