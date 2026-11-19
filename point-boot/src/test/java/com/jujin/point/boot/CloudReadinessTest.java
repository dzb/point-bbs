package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.cloud.CloudModule;
import com.jujin.freeway.cloud.discovery.StaticModule;
import com.jujin.freeway.http.HttpModule;
import com.jujin.point.boot.cloud.AuthRpcExportModule;
import com.jujin.point.boot.cloud.CloudHealthModule;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Cloud probe contract.
 *
 * <p>{@code /health/ready} must answer JSON from freeway's readiness route and
 * report the application's database contributor. That single assertion covers
 * two failure modes that used to hide behind each other: with no contributor
 * the aggregate is always OK, and while the old {@code SpaFilter} ran before
 * route dispatch the probe answered {@code 200 text/html} (the SPA shell) for
 * a path no route had handled.</p>
 */
class CloudReadinessTest {

    private static final String SERVICE_ID = "point-ready-test";

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
        System.clearProperty("F2.cloud.discovery.static.services");
    }

    @Test
    void readinessReportsTheDatabaseContributor() throws Exception {
        int port = freePort();
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, String.valueOf(port));
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_ready_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
        System.setProperty(
            "F2.cloud.discovery.static.services",
            SERVICE_ID + "=http://127.0.0.1:" + port
        );

        app = FreewayApp.create(PointModules.base())
            .name(SERVICE_ID)
            .add(new AuthRpcExportModule())
            .add(new CloudHealthModule())
            .add(CloudModule.class)
            .add(StaticModule.class)
            .autoDiscovery(false)
            .start();

        var ready = get(port, "/health/ready");
        assertEquals(200, ready.statusCode(), "a reachable database makes the instance ready");
        assertTrue(
            ready.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
            "readiness answers JSON, not the SPA shell"
        );
        assertTrue(ready.body().contains("\"status\":\"ok\""), ready.body());
        assertTrue(
            ready.body().contains(CloudHealthModule.DB_CONTRIBUTOR),
            "the aggregation names the database contributor: " + ready.body()
        );

        var live = get(port, "/health/live");
        assertEquals(200, live.statusCode());
        assertTrue(
            live.headers().firstValue("Content-Type").orElse("").startsWith("application/json"),
            "liveness is a probe route too — the SPA fallback must not swallow it"
        );
        assertTrue(live.body().contains("ok"), live.body());
    }

    private static HttpResponse<String> get(int port, String path) throws Exception {
        return HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
