package com.remote.system_pulse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.remote.system_pulse.model.User;
import com.remote.system_pulse.model.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    /*
     * JwtService has no injectable dependencies (no repositories, no other services).
     * Its only "dependencies" are @Value fields (secret, expirationMs) which Mockito
     * can't inject — so we use ReflectionTestUtils.setField() in @BeforeEach.
     *
     * There are no mocks to verify(). Instead, we generate real tokens and decode
     * them to assert the contents are correct.
     */

    @InjectMocks
    private JwtService jwtService;

    // A valid Base64-encoded 256-bit key for HS256 signing
    private static final String TEST_SECRET = Base64.getEncoder()
            .encodeToString("this-is-a-test-secret-key-32bytes!".getBytes());
    private static final long TEST_EXPIRATION_MS = 86400000L; // 1 day

    @BeforeEach
    void setUp() {
        // Inject the @Value fields that Mockito can't handle
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", TEST_EXPIRATION_MS);
    }

    // ========================
    // Helper
    // ========================

    private User buildTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setRole(UserRole.ADMIN);
        user.setName("Test User");
        return user;
    }

    private SecretKey testKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
    }

    // ========================
    // generate() tests
    // ========================

    @Test
    @DisplayName("Should generate a valid JWT with correct subject and claims")
    void generate_ShouldReturnTokenWithCorrectClaims_WhenGivenValidUser() {
        // Arrange
        User user = buildTestUser();

        // Act
        String token = jwtService.generate(user);

        // Assert — token exists
        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Decode the token independently and verify each claim
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(testKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals("test@example.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("Test User", claims.get("name", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when user is null")
    void generate_ShouldThrowException_WhenUserIsNull() {
        // generate() calls user.getEmail() — NPE on null
        assertThrows(NullPointerException.class, () -> {
            jwtService.generate(null);
        });
    }

    // ========================
    // extractUserEmail() tests
    // ========================

    @Test
    @DisplayName("Should extract the correct email from a valid token")
    void extractUserEmail_ShouldReturnEmail_WhenTokenIsValid() {
        // Arrange — generate a real token first
        String token = jwtService.generate(buildTestUser());

        // Act
        String email = jwtService.extractUserEmail(token);

        // Assert
        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("Should throw exception when token is signed with a different key")
    void extractUserEmail_ShouldThrowException_WhenSignatureIsWrong() {
        // Arrange — build a token signed with a DIFFERENT secret
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String tamperedToken = Jwts.builder()
                .setSubject("hacker@example.com")
                .signWith(wrongKey)
                .compact();

        // Act & Assert — our service should reject it
        assertThrows(Exception.class, () -> {
            jwtService.extractUserEmail(tamperedToken);
        });
    }

    @Test
    @DisplayName("Should throw exception when token string is garbage")
    void extractUserEmail_ShouldThrowException_WhenTokenIsMalformed() {
        assertThrows(Exception.class, () -> {
            jwtService.extractUserEmail("not.a.real.token");
        });
    }

    // ========================
    // isValid() tests
    // ========================

    @Test
    @DisplayName("Should return true for a valid non-expired token")
    void isValid_ShouldReturnTrue_WhenTokenIsValid() {
        // Arrange
        String token = jwtService.generate(buildTestUser());

        // Act & Assert
        assertTrue(jwtService.isValid(token));
    }

    @Test
    @DisplayName("Should return false for an expired token")
    void isValid_ShouldReturnFalse_WhenTokenIsExpired() {
        // Arrange — build a token that expired 1 hour ago
        String expiredToken = Jwts.builder()
                .setSubject("test@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))  // 2h ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1h ago
                .signWith(testKey())
                .compact();

        // Act & Assert
        assertFalse(jwtService.isValid(expiredToken));
    }

    @Test
    @DisplayName("Should return false for a token signed with a wrong key")
    void isValid_ShouldReturnFalse_WhenSignatureIsInvalid() {
        // Arrange
        SecretKey wrongKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String badToken = Jwts.builder()
                .setSubject("test@example.com")
                .signWith(wrongKey)
                .compact();

        // Act & Assert
        assertFalse(jwtService.isValid(badToken));
    }

    @Test
    @DisplayName("Should return false for a malformed token string")
    void isValid_ShouldReturnFalse_WhenTokenIsMalformed() {
        assertFalse(jwtService.isValid("totally.broken.token"));
    }

    @Test
    @DisplayName("Should return false when token is null")
    void isValid_ShouldReturnFalse_WhenTokenIsNull() {
        assertFalse(jwtService.isValid(null));
    }
}
