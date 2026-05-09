# Test Coverage Analysis — System Pulse

## Current coverage map (manual — performed by Claude Opus)

| Component | Status | Notes |
|---|---|---|
| `ServerService` | ✅ 8 tests | Well covered |
| `ServerTelemetryService` | ✅ 5 tests | Well covered |
| `UserService` | ✅ 8 tests | Well covered |
| `JwtService` | ❌ | **Real logic**, should have tests |
| `NotificationService` | ❌ | Thin wrapper |
| `CustomUserDetailsService` | ❌ | Small |
| `AuthService` | — | Empty class, nothing to test |
| `AuthController` | ❌ | No tests |
| `ServerController` | ❌ | No tests |
| `UsersController` | ❌ | No tests |
| `ServerRepository.findServerByToken` | ❌ | Custom query |
| `UserRepository.findByEmail` | ❌ | Custom query |
| `IpRegex` | ❌ | Pure regex, easy to test |
| `JwtCookieFilter` / `SecurityConfig` | ❌ | Security filter |

**To measure real coverage:** add the **JaCoCo** plugin to `pom.xml` (generates an HTML report at `target/site/jacoco/index.html`).

---

## Types of tests missing from the project

Spring Boot has a very clear hierarchy — only level 1 has been done:

### 1. **Unit tests** ✅ (what's already done)
Mockito + JUnit. Fast, isolated, no Spring context.

### 2. **Slice tests** (natural next step)
They boot only a "slice" of Spring — faster than full integration:

- **`@WebMvcTest`** — tests controllers without starting the database/services. Uses `MockMvc` to simulate HTTP requests. Validates deserialization, status codes, DTO validation, error messages.
  ```java
  @WebMvcTest(ServerController.class)
  // mock services with @MockBean, make requests with mockMvc.perform(post(...))
  ```

- **`@DataJpaTest`** — tests repositories with an in-memory H2 database. Ideal for testing custom queries like `findServerByToken` and `findByEmail`.

- **`@JsonTest`** — tests DTO serialization/deserialization (rarely useful at the beginning).

### 3. **Integration tests** (`@SpringBootTest`)
Boots the entire context. Tests the full end-to-end flow. Slower, use sparingly.

- Combine with **Testcontainers** to spin up a real Postgres (instead of H2) — makes it identical to production.
- `ApplicationTests` already exists — it's just a smoke test ("does the context start?"). It can be evolved.

### 4. **Security tests**
`spring-security-test` gives you `@WithMockUser`, `@WithUserDetails`. Essential now that JWT, login, and roles exist. Without this, authorization can't be tested.

---

## About TDD — honest advice

TDD is not "writing more tests". It's a **workflow**: red → green → refactor. You write the test **before** the production code.

**Where TDD doesn't make sense here:** going back to existing services and "doing TDD" on them. The code is already written — you're just testing after the fact.

**Where TDD makes a lot of sense from now on:**

1. **Bug fix** — when a bug appears, write a test that **reproduces** the bug first (it goes red), then fix the code (it goes green). Guarantees it never comes back.

2. **New feature** — examples from your project:
   - "Filter servers by status" in `ServerService`
   - "Find servers by CPU > X%"
   - Password change endpoint

   For each one: write the expectation as a test first, watch it fail, implement the minimum to pass, refactor.

3. **Business rule validation** — e.g.: "don't allow registering 2 servers with the same name". Test first, then the code.

**Good candidate to start with now:** `IpRegex`. It's pure code, no dependencies. You can write:
```java
@ParameterizedTest
@ValueSource(strings = {"192.168.0.1", "10.0.0.1", "::1", "2001:db8::1"})
void shouldAcceptValidIps(String ip) { ... }

@ParameterizedTest
@ValueSource(strings = {"999.999.999.999", "abc", ""})
void shouldRejectInvalidIps(String ip) { ... }
```
But note: `IpRegex` **isn't being used anywhere** yet. If you're going to add `@Pattern(regexp = IpRegex.IP_PATTERN)` validation to some DTO, **then TDD makes sense**: test the expected behavior first.

---

## Recommended practical sequence

1. **Add JaCoCo** — to have real coverage data, not guesswork.
2. **`@DataJpaTest` for the 2 custom repository methods** — learn slice testing in a small scope.
3. **`@WebMvcTest` for `ServerController`** — learn to test HTTP, validation, status codes.
4. **`JwtService` test** — pure unit test, but different: you'll need a mocked `@Value` or use `ReflectionTestUtils`. Good challenge.
5. **TDD for the next feature or bug** — don't force it, wait for the natural opportunity.
