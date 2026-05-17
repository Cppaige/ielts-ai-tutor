package com.ielts.gateway.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private SecretKey key;
    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long";

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validate_validToken_returnsUserId() {
        String token = Jwts.builder()
                .subject("42")
                .claim("email", "test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        var result = jwtValidator.validate(token);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
    }

    @Test
    void validate_expiredToken_returnsEmpty() {
        String token = Jwts.builder()
                .subject("42")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        var result = jwtValidator.validate(token);
        assertTrue(result.isEmpty());
    }

    @Test
    void validate_invalidToken_returnsEmpty() {
        var result = jwtValidator.validate("garbage.token.here");
        assertTrue(result.isEmpty());
    }
}
