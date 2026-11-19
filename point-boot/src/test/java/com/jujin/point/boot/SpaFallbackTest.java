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
 * The SPA shell is served by the static mount's fallback, not by a filter.
 *
 * <p>freeway's {@code withSpaFallback} is the terminal hop of nginx's
 * {@code try_files}: it answers an extensionless GET/HEAD only after the mount
 * and the route chain have both missed. The filter it replaced ran before
 * route dispatch, which is how {@code /health/live} and {@code /health/ready}
 * came back as HTML in the cloud shape.</p>
 */
class SpaFallbackTest {

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
    void extensionlessPathsFallBackToTheShellButRoutesAndAssetsDoNot() throws Exception {
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, "0");
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_spa_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
        app = FreewayApp.create(PointModules.base())
            .name("point-spa-test")
            .autoDiscovery(false)
            .start();
        int port = app.get(HttpServer.class).port();

        var root = get(port, "/");
        assertEquals(200, root.statusCode());
        assertTrue(root.headers().firstValue("Content-Type").orElse("").startsWith("text/html"));

        // Extensionless client route with no server route: the shell.
        var deepLink = get(port, "/topic/42");
        assertEquals(200, deepLink.statusCode());
        assertTrue(
            deepLink.headers().firstValue("Content-Type").orElse("").startsWith("text/html"),
            "an unknown extensionless path is a client route"
        );

        // A dotted last segment is an asset: missing means 404, never the shell.
        assertEquals(404, get(port, "/assets/definitely-missing.js").statusCode());

        // Real routes still win over the fallback.
        var healthz = get(port, "/healthz");
        assertEquals(200, healthz.statusCode());
        assertTrue(
            healthz.headers().firstValue("Content-Type").orElse("").startsWith("application/json")
        );
        var api = get(port, "/api/topics");
        assertEquals(200, api.statusCode());
        assertTrue(api.headers().firstValue("Content-Type").orElse("").startsWith("application/json"));

        // Only GET/HEAD fall back; an unknown POST stays a 404.
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
