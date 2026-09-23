package com.jujin.point.domain.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Password hashing — the single owner of the stored-hash format.
 *
 * <p>Current format: {@code pbkdf2$<iterations>$<saltHex>$<hashHex>} —
 * PBKDF2WithHmacSHA256 (OWASP-recommended KDF) with a per-user random
 * 16-byte salt. The legacy single-round SHA-256 format
 * {@code saltHex:hashHex} is still accepted by {@link #verify} and
 * transparently upgraded by the caller on the next successful login.
 */
public final class Passwords {

    private static final int PBKDF2_ITERATIONS = 120_000;

    private Passwords() {}

    /** Returns a stored hash of the current pbkdf2 format for the password. */
    public static String hash(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            var spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                256
            );
            var factory = javax.crypto.SecretKeyFactory.getInstance(
                "PBKDF2WithHmacSHA256"
            );
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return (
                "pbkdf2$" +
                PBKDF2_ITERATIONS +
                "$" +
                HexFormat.of().formatHex(salt) +
                "$" +
                HexFormat.of().formatHex(hash)
            );
        } catch (Exception e) {
            throw new RuntimeException("hash error", e);
        }
    }

    /** True when the stored hash verifies against the password (both formats). */
    public static boolean verify(String password, String stored) {
        if (stored == null) return false;
        if (isCurrentFormat(stored)) {
            try {
                var parts = stored.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = HexFormat.of().parseHex(parts[2]);
                byte[] expected = HexFormat.of().parseHex(parts[3]);
                var spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    expected.length * 8
                );
                var factory = javax.crypto.SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
                );
                byte[] actual = factory.generateSecret(spec).getEncoded();
                return MessageDigest.isEqual(actual, expected);
            } catch (Exception e) {
                return false;
            }
        }
        // Legacy single-round SHA-256 format (salt:hash)
        if (!stored.contains(":")) return false;
        try {
            var parts = stored.split(":");
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return HexFormat.of().formatHex(hash).equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }

    /** True when the stored hash uses the current PBKDF2 format. */
    public static boolean isCurrentFormat(String stored) {
        return stored != null && stored.startsWith("pbkdf2$");
    }
}
