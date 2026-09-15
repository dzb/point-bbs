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
import com.jujin.freeway.http.HttpConfigKeys;
import com.jujin.freeway.http.WebServer;
import com.jujin.freeway.ioc.ModuleNode;
import com.jujin.point.boot.cloud.AuthRpcExportModule;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Cloud-readiness contract for the auth export:
 *
 * <ul>
 *   <li>mesh shape (base tree + CloudModule + {@link AuthRpcExportModule}):
 *       a peer resolves a session token over {@code POST /rpc/auth/validateToken}
 *       — token in, identity out, including the wire-safe null for a bogus
 *       token;</li>
 *   <li>monolith shape (the exact {@link PointApp} tree, which is what local
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
        System.clearProperty(HttpConfigKeys.SERVER_PORT);
        System.clearProperty(HttpConfigKeys.SERVER_HOST);
        System.clearProperty("freeway.db.url");
        System.clearProperty("bbs.jwt.secret");
    }

    private static ModuleNode tree(ModuleNode... extra) {
        var children = new ArrayList<ModuleNode>(List.of(PointModules.base()));
        children.addAll(List.of(extra));
        return ModuleNode.app("point-test", children.toArray(ModuleNode[]::new));
    }

    private static void setCommonConfig() {
        System.setProperty(HttpConfigKeys.SERVER_PORT, "0");
        System.setProperty(HttpConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_rpc_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
    }

    private static AppRuntime startTree(ModuleNode tree) {
        // Same explicit composition as PointApp/PointCloudApp: autoDiscovery
        // is off, the tree is the whole story.
        return FreewayApp.create(tree).autoDiscovery(false).start();
    }

    @Test
    void meshShapeValidatesTokensOverRpc() throws Exception {
        setCommonConfig();
        app = startTree(tree(
            ModuleNode.of(new AuthRpcExportModule()),
            ModuleNode.of(com.jujin.freeway.cloud.CloudModule.class)
        ));

        var token = app.get(AuthService.class)
            .createToken(7L, "rpcuser", "avatar-url", Set.of("admin"));

        // The test stands in for a real discovery backend: point the registry
        // at this node's own endpoint (same move freeway-cloud's own wiring
        // tests make).
        var web = app.get(WebServer.class);
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
        app = startTree(tree()); // exactly the PointApp composition

        var web = app.get(WebServer.class);
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
