package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.cloud.discovery.StaticModule;
import com.jujin.freeway.cloud.rpc.RemoteCaller;
import com.jujin.freeway.http.HttpModule;
import com.jujin.freeway.http.HttpServer;
import com.jujin.point.boot.cloud.AuthRpcExportModule;
import com.jujin.point.boot.cloud.CloudHealthModule;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.domain.dto.UserDtos.CreateUserRequest;
import com.jujin.point.domain.entity.Role;
import com.jujin.point.service.AuthService;
import com.jujin.point.service.PermissionService;
import com.jujin.point.service.UserService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Cloud-readiness contract for the auth export:
 *
 * <ul>
 *   <li>cloud shape (base list + CloudModule + {@link StaticModule} +
 *       {@link AuthRpcExportModule}): a peer resolves a session token over
 *       {@code POST /rpc/auth/validateToken} — token in, identity out,
 *       including the wire-safe null for a bogus token;</li>
 *   <li>monolith shape (the exact {@link PointApp} list, which is what local
 *       single-machine deployment runs): the RPC surface does not exist —
 *       {@code /rpc/...} is 404 — proving the cloud classes on the classpath
 *       are inert until the composition installs them.</li>
 * </ul>
 */
class AuthRpcExportTest {

    private static final String SERVICE_ID = "point-test";
    private static final String STATIC_SERVICES =
        "F2.cloud.discovery.static.services";

    private AppRuntime app;

    @AfterEach
    void stop() {
        if (app != null) {
            app.close();
            app = null;
        }
        System.clearProperty(HttpModule.ConfigKeys.SERVER_PORT);
        System.clearProperty(HttpModule.ConfigKeys.SERVER_HOST);
        System.clearProperty(STATIC_SERVICES);
        System.clearProperty("freeway.db.url");
        System.clearProperty("bbs.jwt.secret");
    }

    private static AppRuntime startTree(boolean cloud) {
        // Same explicit composition as PointApp/PointCloudApp: autoDiscovery
        // is off, the declaration list is the whole story.
        var launcher = FreewayApp.create(PointModules.base()).name("point-test");
        if (cloud) {
            launcher.add(new AuthRpcExportModule());
            launcher.add(new CloudHealthModule());
            launcher.add(com.jujin.freeway.cloud.CloudModule.class);
            // freeway 2.0.1: config-driven static discovery, no app module.
            launcher.add(StaticModule.class);
        }
        return launcher.autoDiscovery(false).start();
    }

    /**
     * Static config must name an explicit port before the HTTP server starts,
     * so the cloud shape picks a free port up front instead of binding :0.
     */
    private static int setCommonConfig(boolean cloud) throws IOException {
        int port = cloud ? freePort() : 0;
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, String.valueOf(port));
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_rpc_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
        if (cloud) {
            System.setProperty(
                STATIC_SERVICES,
                SERVICE_ID + "=http://127.0.0.1:" + port);
        }
        return port;
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    void cloudShapeResolvesPermissionsOverRpc() throws Exception {
        int port = setCommonConfig(true);
        app = startTree(true);
        assertEquals(port, app.get(HttpServer.class).port(),
            "the cloud shape binds the endpoint named in static discovery");

        var users = app.get(UserService.class);
        var permissions = app.get(PermissionService.class);
        var auth = app.get(AuthService.class);
        long adminRoleId = permissions.getAllRoles().stream()
            .filter(role -> "admin".equals(role.getCode()))
            .map(Role::getId)
            .findFirst()
            .orElseThrow(() -> new AssertionError("the seeded admin role is missing"));

        var admin = users.signUp(new CreateUserRequest(
            "rpcadmin", "rpc-admin@example.com", null, "rpcadmin",
            "secret123", null, null, null));
        permissions.assignRole(admin.getId(), adminRoleId);

        // Exactly what /api/auth/signin issues: a token with no role claim.
        // Permissions must be resolved at validation time, not read from it.
        var adminToken = auth.createToken(admin.getId(), "rpcadmin", "", null);
        var current = app.get(RemoteCaller.class)
            .invoke(SERVICE_ID, "auth", "validateToken", List.of(adminToken), CurrentUser.class);
        assertEquals(admin.getId(), current.userId(), "the export resolves the presented token");
        assertTrue(current.isAdmin(),
            "an admin's permission codes ride the wire even though the JWT has no role claim");
        assertTrue(current.roles().contains("admin"),
            "roles carries permission codes, matching AuthFilter's local identity");

        var plain = users.signUp(new CreateUserRequest(
            "rpcplain", "rpc-plain@example.com", null, "rpcplain",
            "secret123", null, null, null));
        var plainToken = auth.createToken(plain.getId(), "rpcplain", "", null);
        var plainUser = app.get(RemoteCaller.class)
            .invoke(SERVICE_ID, "auth", "validateToken", List.of(plainToken), CurrentUser.class);
        assertFalse(plainUser.isAdmin(), "a user with no role is not an admin over RPC");

        var bogus = app.get(RemoteCaller.class)
            .invoke(SERVICE_ID, "auth", "validateToken", List.of("x.y.z"), CurrentUser.class);
        assertNull(bogus, "an invalid token stays null across the wire, not an error");
    }

    @Test
    void monolithShapeHasNoRpcSurface() throws Exception {
        setCommonConfig(false);
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
