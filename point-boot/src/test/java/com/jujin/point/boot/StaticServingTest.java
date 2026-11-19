package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.http.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Where the shell and the API boundaries sit.
 *
 * <ul>
 *   <li>{@code /} serves the SPA shell through the root mount's directory rule
 *       — the hash router never asks the server for another path;</li>
 *   <li>a miss under {@code /api} (or any unknown path) stays a JSON 404. A
 *       terminal SPA fallback would answer it with the HTML shell, which is
 *       what the old filter-and-fallback arrangement used to do for API
 *       typos, and what a client cannot parse.</li>
 * </ul>
 */
class StaticServingTest {

    private AppRuntime app;

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
    void shellIsServedAtRootAndUnknownPathsStayJson404() throws Exception {
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, "0");
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_static_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
        app = FreewayApp.create(PointModules.base())
            .name("point-static-test")
            .autoDiscovery(false)
            .start();
        int port = app.get(HttpServer.class).port();

        var root = get(port, "/");
        assertEquals(200, root.statusCode());
        assertTrue(root.headers().firstValue("Content-Type").orElse("").startsWith("text/html"),
            "the SPA shell is the mount's index.html");

        var apiMiss = get(port, "/api/topicz");
        assertEquals(404, apiMiss.statusCode(),
            "an unknown API path is a 404, never the SPA shell");
        assertTrue(apiMiss.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        assertEquals(404, get(port, "/api").statusCode());

        // Real routes still win over the mount.
        var healthz = get(port, "/healthz");
        assertEquals(200, healthz.statusCode());
        assertTrue(healthz.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));
        var api = get(port, "/api/topics");
        assertEquals(200, api.statusCode());
        assertTrue(api.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));

        // Dotted paths are assets: missing means 404, never the shell.
        assertEquals(404, get(port, "/assets/definitely-missing.js").statusCode());

        // Unknown extensionless client paths are 404 too: the router is
        // hash-based, so the server only ever serves "/".
        assertEquals(404, get(port, "/topicz").statusCode());

        var post = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/no-such-route"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, post.statusCode());
    }

    private static HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }
}
