package com.jujin.point.service;

import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.freeway.ioc.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

/**
 * JWT token service for authentication.
 * Uses HMAC-SHA256 with simple claims encoding.
 */
public class AuthService {
    private final String secret;
    private final int expireDays;

    public AuthService(
        @Value("${bbs.jwt.secret:change-me}") String secret,
        @Value("${bbs.jwt.expire-days:7}") int expireDays
    ) {
        this.secret = secret;
        this.expireDays = expireDays;
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
            if (!sign(data).equals(sig)) return Optional.empty();

            var payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = extractLong(payload, "exp");
            if (System.currentTimeMillis() / 1000 > exp) return Optional.empty();

            long userId = Long.parseLong(extractString(payload, "sub"));
            String nickname = extractString(payload, "nickname");
            String avatar = extractString(payload, "avatar");
            String rolesStr = extractString(payload, "roles");
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
            .replace("\n", "\\n");
    }

    private static String extractString(String json, String key) {
        var pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) {
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start < 0) return null;
            int valStart = start + pattern.length();
            int valEnd = json.indexOf(",", valStart);
            if (valEnd < 0) valEnd = json.indexOf("}", valStart);
            return json.substring(valStart, valEnd);
        }
        int valStart = start + pattern.length();
        int valEnd = json.indexOf("\"", valStart);
        return json.substring(valStart, valEnd);
    }

    private static long extractLong(String json, String key) {
        var s = extractString(json, key);
        return s != null ? Long.parseLong(s) : 0;
    }
}
