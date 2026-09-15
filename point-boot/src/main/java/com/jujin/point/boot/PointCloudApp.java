package com.jujin.point.boot;

import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.cloud.CloudModule;
import com.jujin.freeway.ioc.ModuleNode;
import com.jujin.point.boot.cloud.AuthRpcExportModule;

/**
 * point in the cloud — the mesh deployment shape of the same application.
 *
 * Composes the identical base module tree as {@link PointApp} plus the cloud
 * bundle and the auth export declaration: the node registers itself in
 * discovery and serves {@code POST /rpc/auth/validateToken} for peers.
 * Business code and the local {@code AuthApi} seam (in-process,
 * ScopedValue-based identity) are unchanged — cross-process peers ride RPC,
 * this process keeps answering "who is the current request" from its own
 * request thread.
 *
 * <p>Run with the point-boot fat jar:
 * {@code java -cp point-boot.jar com.jujin.point.boot.PointCloudApp}.
 * Discovery backend, service id and TLS are configured through
 * {@code freeway.cloud.*} keys — see freeway's docs/freeway-config.md.</p>
 */
public class PointCloudApp {

    public static void main(String[] args) throws Exception {
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));
        var logConfig = PointCloudApp.class
            .getClassLoader()
            .getResource("logging.properties");
        if (logConfig != null) {
            java.util.logging.LogManager.getLogManager().readConfiguration(
                logConfig.openStream()
            );
        }

        var base = PointModules.base();
        var children = new ModuleNode[base.length + 2];
        System.arraycopy(base, 0, children, 0, base.length);
        children[base.length] = ModuleNode.of(new AuthRpcExportModule());
        children[base.length + 1] = ModuleNode.of(CloudModule.class);

        var runtime = FreewayApp.create(ModuleNode.app("point", children))
            .autoDiscovery(false) // Same explicit composition as the monolith
            .args(args)
            .start();

        var port = runtime
            .get(com.jujin.freeway.ioc.symbol.SymbolSource.class)
            .resolve("freeway.http.server.port", null);
        System.out.println(
            "point (cloud shape) running on http://localhost:" +
            (port != null ? port : 8082) +
            " — auth exported as /rpc/auth/validateToken"
        );

        // Keep JVM alive — freeway HTTP acceptor threads are daemon.
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
