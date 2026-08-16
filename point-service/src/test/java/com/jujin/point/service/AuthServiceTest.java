package com.jujin.point.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private static final String SECRET = "test-secret-0123456789-abcdef";

    private AuthService svc() {
        return new AuthService(SECRET, 7, "", "");
    }

    @Test
    void tokenRoundTrip() {
        var svc = svc();
        var token = svc.createToken(42L, "墨客", "http://a/1.png", java.util.Set.of("admin"));
        var user = svc.validateToken(token);
        assertTrue(user.isPresent());
        assertEquals(42L, user.get().userId());
        assertEquals("墨客", user.get().nickname());
        assertEquals("http://a/1.png", user.get().avatar());
        assertEquals(java.util.Set.of("admin"), user.get().roles());
    }

    @Test
    void rejectsTamperedSignature() {
        var svc = svc();
        var token = svc.createToken(42L, "墨客", null, null);
        // Flip one char in the payload segment
        var parts = token.split("\\.");
        var tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 1)
            + (parts[1].endsWith("A") ? "B" : "A") + "." + parts[2];
        assertNotEquals(token, tampered);
        assertTrue(svc.validateToken(tampered).isEmpty());
    }

    @Test
    void rejectsWrongSecret() {
        var token = svc().createToken(42L, "墨客", null, null);
        var other = new AuthService("a-completely-different-secret-123456", 7, "", "");
        assertTrue(other.validateToken(token).isEmpty());
    }

    @Test
    void rejectsExpiredToken() {
        var svc = new AuthService(SECRET, -1, "", ""); // already expired on creation
        var token = svc.createToken(42L, "墨客", null, null);
        assertTrue(svc.validateToken(token).isEmpty());
    }

    @Test
    void rejectsMalformedToken() {
        var svc = svc();
        assertTrue(svc.validateToken("not-a-jwt").isEmpty());
        assertTrue(svc.validateToken("a.b").isEmpty());
        assertTrue(svc.validateToken("a.b.c.d").isEmpty());
        assertTrue(svc.validateToken(null).isEmpty());
    }

    @Test
    void nicknameEscapingSurvivesRoundTrip() {
        var svc = svc();
        var token = svc.createToken(1L, "a\"b\\c\nd", null, null);
        var user = svc.validateToken(token);
        assertTrue(user.isPresent());
        assertEquals("a\"b\\c\nd", user.get().nickname());
    }

    @Test
    void rejectsPlaceholderSecretsOnProdProfile() {
        assertThrows(IllegalStateException.class,
            () -> new AuthService("change-me-in-production-use-a-long-random-string", 7, "prod", ""));
        assertThrows(IllegalStateException.class,
            () -> new AuthService("", 7, "prod", ""));
        // Non-prod: placeholder is tolerated (warn only)
        assertDoesNotThrow(() -> new AuthService("change-me", 7, "dev", ""));
    }
}
