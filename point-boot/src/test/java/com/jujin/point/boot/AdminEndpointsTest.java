package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.http.HttpServer;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the small fixes around sessions and admin bodies:
 *
 * <ul>
 *   <li>a missing config key is a null value, not a 500 from {@code Map.of};</li>
 *   <li>malformed admin/user bodies answer 400 {@code validation_failed};</li>
 *   <li>the admin user list goes through the service and hides password hashes;</li>
 *   <li>the session cookie's Max-Age follows {@code bbs.jwt.expire-days}
 *       (dev is 30 days), not a hardcoded week;</li>
 *   <li>the notification socket accepts the session cookie and nothing in the
 *       query string.</li>
 * </ul>
 */
class AdminEndpointsTest {

    private AppRuntime app;
    private int port;
    private String token;
    private String cookie;
    private HttpClient client;

    @BeforeEach
    void start() throws Exception {
        port = freePort();
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, String.valueOf(port));
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_admin_endpoints;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "admin-endpoints-secret-1234567890abcd");
        app = FreewayApp.create(PointModules.base())
            .name("point-admin-endpoints")
            .autoDiscovery(false)
            .args("--F2.profile=dev")
            .start();

        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        var signin = post("/api/auth/signin", "{\"loginName\":\"moke\",\"password\":\"123456\"}", null);
        assertEquals(200, signin.statusCode(), signin.body());
        token = com.jujin.freeway.commons.json.JsonUtils
            .parseObject(signin.body())
            .getObject("data")
            .getString("token");
        cookie = signin.headers().firstValue("set-cookie").orElseThrow();
    }

    @AfterEach
    void stop() {
        if (app != null) {
            app.close();
            app = null;
        }
        System.clearProperty(HttpModule.ConfigKeys.SERVER_PORT);
        System.clearProperty(HttpModule.ConfigKeys.SERVER_HOST);
        System.clearProperty("freeway.db.url");
        System.clearProperty("bbs.jwt.secret");
    }

    @Test
    void sessionCookieLifetimeFollowsTheToken() {
        assertTrue(cookie.contains("Max-Age=2592000"),
            "dev expires sessions in 30 days; the cookie must match: " + cookie);
        assertTrue(cookie.contains("HttpOnly"));
    }

    @Test
    void missingConfigKeyAnswersNullValue() throws Exception {
        var response = get("/api/admin/sys-config/not-configured");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"value\":null"), response.body());
    }

    @Test
    void malformedAdminBodyIsAValidationError() throws Exception {
        var response = post("/api/admin/user/update/2", "{\"nickname\":", token);
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("validation_failed"), response.body());
    }

    @Test
    void malformedProfileBodyIsAValidationError() throws Exception {
        var response = post("/api/users/edit/1", "not-json", token);
        assertEquals(400, response.statusCode(), response.body());
        assertTrue(response.body().contains("validation_failed"), response.body());
    }

    @Test
    void adminUserListComesFromTheServiceWithoutHashes() throws Exception {
        var response = get("/api/admin/user?page=1&pageSize=10");
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("\"total\":5"), response.body());
        assertTrue(response.body().contains("\"password\":null"),
            "password hashes must never leave the service boundary");
    }

    @Test
    void notificationSocketAuthenticatesByCookieOnly() throws Exception {
        var base = "ws://127.0.0.1:" + port + "/ws/notify";
        assertEquals(4401, closeCode(base, null), "no credentials must be rejected");
        assertEquals(4401, closeCode(base + "?token=" + token, null),
            "a token in the query string is no longer accepted");
        assertEquals(-1, closeCode(base, cookie),
            "the session cookie authorizes the handshake (-1 means still open)");
    }

    /** -1 when the socket stayed open; otherwise the close status. */
    private static int closeCode(String uri, String cookieHeader) throws Exception {
        var latch = new CountDownLatch(1);
        var code = new AtomicInteger(-1);
        var builder = HttpClient.newHttpClient().newWebSocketBuilder();
        if (cookieHeader != null) {
            builder.header("Cookie", cookieHeader);
        }
        var socket = builder.buildAsync(URI.create(uri), new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onClose(WebSocket ws, int status, String reason) {
                code.set(status);
                latch.countDown();
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                latch.countDown();
            }
        }).join();
        if (!latch.await(1500, TimeUnit.MILLISECONDS)) {
            socket.sendClose(1000, "done");
            return -1;
        }
        return code.get();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(String path, String body, String bearer) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
