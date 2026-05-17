package com.ielts.data.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-that-is-at-least-32-bytes-long", 7200000L);
    }

    @Test
    void generateToken_containsUserId() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertNotNull(token);
        assertEquals(1L, jwtUtil.extractUserId(token));
    }

    @Test
    void generateToken_containsEmail() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(1L, "test@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }
}
