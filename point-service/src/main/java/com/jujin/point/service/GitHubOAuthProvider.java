package com.jujin.point.service;

import com.jujin.freeway.ioc.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GitHub OAuth provider.
 */
public class GitHubOAuthProvider implements OAuthProvider {
    private final String clientId;
    private final String clientSecret;
    private final HttpClient http = HttpClient.newHttpClient();

    public GitHubOAuthProvider(
        @Value("${bbs.oauth.github.client-id:}") String clientId,
        @Value("${bbs.oauth.github.client-secret:}") String clientSecret
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public String name() { return "github"; }

    @Override
    public String authorizeUrl(String state) {
        return "https://github.com/login/oauth/authorize?client_id=" + clientId +
            "&redirect_uri=" + encode(redirectUri()) + "&state=" + state + "&scope=user:email";
    }

    @Override
    public OAuthUserInfo handleCallback(String code) {
        try {
            // Exchange code for access token
            var tokenReq = HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"client_id\":\"" + clientId + "\",\"client_secret\":\"" + clientSecret +
                    "\",\"code\":\"" + code + "\",\"redirect_uri\":\"" + redirectUri() + "\"}"))
                .build();
            var tokenRes = http.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            var tokenJson = com.jujin.freeway.commons.json.JsonUtils.parseObject(tokenRes.body());
            String accessToken = tokenJson.getString("access_token");
            if (accessToken == null) return null;

            // Get user info
            var userReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET().build();
            var userRes = http.send(userReq, HttpResponse.BodyHandlers.ofString());
            var userJson = com.jujin.freeway.commons.json.JsonUtils.parseObject(userRes.body());

            Long idVal = userJson.getLong("id");
            String openId = idVal != null ? String.valueOf(idVal) : "0";
            String nickname = userJson.getString("login");
            String avatar = userJson.getString("avatar_url");
            return new OAuthUserInfo(openId, nickname, avatar, userRes.body());
        } catch (Exception e) {
            return null;
        }
    }

    private String redirectUri() {
        return "http://localhost:8082/api/login/github/callback";
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
