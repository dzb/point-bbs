package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.cloud.discovery.Endpoint;
import com.jujin.freeway.cloud.discovery.ServiceInstance;
import com.jujin.freeway.cloud.discovery.ServiceRegistry;
import com.jujin.freeway.cloud.rpc.RemoteCaller;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.http.HttpServer;
import com.jujin.point.boot.cloud.AuthRpcExportModule;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Cloud-readiness contract for the auth export:
 *
 * <ul>
 *   <li>mesh shape (base list + CloudModule + {@link AuthRpcExportModule}):
 *       a peer resolves a session token over {@code POST /rpc/auth/validateToken}
 *       — token in, identity out, including the wire-safe null for a bogus
 *       token;</li>
 *   <li>monolith shape (the exact {@link PointApp} list, which is what local
 *       single-machine deployment runs): the RPC surface does not exist —
 *       {@code /rpc/...} is 404 — proving the cloud classes on the classpath
 *       are inert until the composition installs them.</li>
 * </ul>
 */
class AuthRpcExportTest {

    private static final String SERVICE_ID = "point-test";

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

    private static AppRuntime startTree(boolean mesh) {
        // Same explicit composition as PointApp/PointCloudApp: autoDiscovery
        // is off, the declaration list is the whole story. Instances and
        // class declarations mix on the chain directly (freeway 1.5.6).
        var launcher = FreewayApp.create(PointModules.base()).name("point-test");
        if (mesh) {
            launcher.add(new AuthRpcExportModule());
            launcher.add(com.jujin.freeway.cloud.CloudModule.class);
        }
        return launcher.autoDiscovery(false).start();
    }

    private static void setCommonConfig() {
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, "0");
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_rpc_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
    }

    @Test
    void meshShapeValidatesTokensOverRpc() throws Exception {
        setCommonConfig();
        app = startTree(true);

        var token = app.get(AuthService.class)
            .createToken(7L, "rpcuser", "avatar-url", Set.of("admin"));

        // The test stands in for a real discovery backend: point the registry
        // at this node's own endpoint (same move freeway-cloud's own wiring
        // tests make).
        var web = app.get(HttpServer.class);
        app.get(ServiceRegistry.class)
            .register(ServiceInstance.of(
                SERVICE_ID, "i1", Endpoint.of("http", web.host(), web.port()), Map.of()));

        var current = app.get(RemoteCaller.class)
            .invoke(SERVICE_ID, "auth", "validateToken", List.of(token), CurrentUser.class);
        assertEquals(7L, current.userId(), "the export resolves the presented token");
        assertEquals("rpcuser", current.nickname());
        assertTrue(current.isAdmin(), "roles ride the wire on CurrentUser");

        var bogus = app.get(RemoteCaller.class)
            .invoke(SERVICE_ID, "auth", "validateToken", List.of("x.y.z"), CurrentUser.class);
        assertNull(bogus, "an invalid token stays null across the wire, not an error");
    }

    @Test
    void monolithShapeHasNoRpcSurface() throws Exception {
        setCommonConfig();
        app = startTree(false); // exactly the PointApp composition

        var web = app.get(HttpServer.class);
        var response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + web.port() + "/rpc/auth/validateToken"))
                .POST(HttpRequest.BodyPublishers.ofString("[\"anything\"]"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(),
            "without CloudModule installed there is no /rpc route — local deploy is untouched");
    }
}
