package com.jujin.point.service;

import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.freeway.ioc.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * JWT token service for authentication.
 * Uses HMAC-SHA256 with simple claims encoding.
 */
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final String secret;
    private final int expireDays;

    public AuthService(
        @Value("${bbs.jwt.secret:}") String secret,
        @Value("${bbs.jwt.expire-days:7}") int expireDays,
        @Value("${freeway.profile:}") String profile,
        @Value("${bbs.jwt.cookie-domain:}") String cookieDomain
    ) {
        // A known/placeholder HMAC key lets anyone forge tokens for any userId.
        var effective = secret == null ? "" : secret.trim();
        boolean placeholder = effective.isEmpty() || isPlaceholder(effective);
        if (placeholder) {
            boolean prod = profile != null && profile.contains("prod");
            if (prod) {
                throw new IllegalStateException(
                    "bbs.jwt.secret is missing or still a placeholder — set a " +
                    "random secret of at least 32 chars (e.g. via -Dbbs.jwt.secret=...)"
                );
            }
            log.warn(
                "bbs.jwt.secret is missing or a placeholder — JWT tokens " +
                "are forgeable. Set a random secret before deploying."
            );
        }
        this.secret = effective;
        this.expireDays = expireDays;
        this.cookieDomain = cookieDomain == null ? "" : cookieDomain.trim();
    }

    private final String cookieDomain;

    public static final String COOKIE_NAME = "point_token";

    /**
     * Set-Cookie value for the session token: HttpOnly + SameSite=Lax.
     * SameSite=Lax keeps the cookie off cross-site POST/XHR requests, which
     * is the CSRF protection for all state-changing endpoints (they are all
     * POST). The optional Domain (dev: localhost so the Vite proxy origin
     * receives it) is configured via bbs.jwt.cookie-domain.
     */
    public String sessionCookie(String token, int maxAgeSeconds) {
        var sb = new StringBuilder(COOKIE_NAME)
            .append('=').append(token)
            .append("; Path=/; HttpOnly; SameSite=Lax; Max-Age=").append(maxAgeSeconds);
        if (!cookieDomain.isEmpty()) {
            sb.append("; Domain=").append(cookieDomain);
        }
        return sb.toString();
    }

    /** Set-Cookie value that deletes the session cookie. */
    public String clearSessionCookie() {
        var sb = new StringBuilder(COOKIE_NAME)
            .append("=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0");
        if (!cookieDomain.isEmpty()) {
            sb.append("; Domain=").append(cookieDomain);
        }
        return sb.toString();
    }

    /** Extract the session token from a Cookie header, or null. */
    public static String tokenFromCookie(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String part : cookieHeader.split(";")) {
            var kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(COOKIE_NAME)) {
                return kv[1];
            }
        }
        return null;
    }

    private static boolean isPlaceholder(String secret) {
        return secret.equals("change-me") ||
            secret.equals("change-me-in-production-use-a-long-random-string") ||
            secret.equals("dev-secret-do-not-use-in-production") ||
            secret.equals("SET_A_LONG_RANDOM_SECRET_AT_LEAST_32_CHARS");
    }

    /**
     * Create a JWT-like token for a user.
     * Format: base64(header).base64(payload).base64(signature)
     */
    public String createToken(long userId, String nickname, String avatar, Set<String> roles) {
        var now = Instant.now();
        var exp = now.plusSeconds(expireDays * 86400L);

        var header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        var payload = "{\"sub\":\"" + userId + "\"," +
            "\"nickname\":\"" + escape(nickname) + "\"," +
            "\"avatar\":\"" + escape(avatar) + "\"," +
            "\"roles\":\"" + (roles != null ? String.join(",", roles) : "") + "\"," +
            "\"iat\":" + now.getEpochSecond() + "," +
            "\"exp\":" + exp.getEpochSecond() + "}";

        String data = b64(header) + "." + b64(payload);
        String sig = sign(data);
        return data + "." + sig;
    }

    /**
     * Validate and decode a token into a CurrentUser.
     */
    public Optional<CurrentUser> validateToken(String token) {
        try {
            var parts = token.split("\\.");
            if (parts.length != 3) return Optional.empty();

            String data = parts[0] + "." + parts[1];
            String sig = parts[2];
            // Constant-time comparison to avoid HMAC timing side-channels
            if (!MessageDigest.isEqual(
                sign(data).getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8)
            )) {
                return Optional.empty();
            }

            // Parse with the framework JSON codec — the hand-rolled string
            // scanning used to break on escaped quotes/backslashes in claims.
            var payloadJson = com.jujin.freeway.commons.json.JsonUtils.parseObject(
                new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
            );
            long exp = payloadJson.getLong("exp") != null ? payloadJson.getLong("exp") : 0;
            if (System.currentTimeMillis() / 1000 > exp) return Optional.empty();

            Long userId = payloadJson.getLong("sub");
            if (userId == null) return Optional.empty();
            String nickname = payloadJson.getString("nickname");
            String avatar = payloadJson.getString("avatar");
            String rolesStr = payloadJson.getString("roles");
            Set<String> roles = rolesStr != null && !rolesStr.isEmpty()
                ? Set.of(rolesStr.split(","))
                : Set.of();

            return Optional.of(new CurrentUser(userId, nickname, avatar, roles));

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String sign(String data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("sign error", e);
        }
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029");
    }

}
