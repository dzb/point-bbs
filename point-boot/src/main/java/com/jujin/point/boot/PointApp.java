package com.jujin.point.boot;

import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.ioc.ModuleNode;

/**
 * point application entry point — the single-machine monolith.
 *
 * freeway 1.5.2: module composition is an explicit ModuleNode tree built at
 * the entry point — the app root holds every feature module in bind order —
 * with autoDiscovery(false) so nothing is silently installed a second time.
 * {@link PointCloudApp} is the mesh deployment shape; it composes this same
 * base tree plus the cloud modules.
 */
public class PointApp {

    public static void main(String[] args) throws Exception {
        // Ensure log directory exists (JUL doesn't auto-create)
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));

        // Load JUL logging config — freeway uses java.util.logging via its SLF4J provider
        var logConfig = PointApp.class
            .getClassLoader()
            .getResource("logging.properties");
        if (logConfig != null) {
            java.util.logging.LogManager.getLogManager().readConfiguration(
                logConfig.openStream()
            );
        }

        System.out.println(
            """
             ____  ____  ____  _  _  ____
            ||P ||||O ||||I ||||N ||||T ||
            ||__||||__||||__||||__||||__||
            |/__\\\\||/__\\\\||/__\\\\||/__\\\\||/__\\\\|
            point v1.0.3 -- powered by freeway 1.5.2-SNAPSHOT + JDK %s
            """.formatted(Runtime.version().feature())
        );

        var runtime = FreewayApp.create(
            ModuleNode.app("point", PointModules.base())
        )
            .autoDiscovery(false) // All modules explicitly composed in the tree
            .args(args)
            .start();

        var port = runtime
            .get(com.jujin.freeway.ioc.symbol.SymbolSource.class)
            .resolve("freeway.http.server.port", null);
        System.out.println(
            "point running on http://localhost:" + (port != null ? port : 8082)
        );
        System.out.println("Press Ctrl+C to stop.");

        // Keep JVM alive — freeway HTTP acceptor threads are daemon.
        // Shutdown is handled by the JVM shutdown hook registered by FreewayApp.
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
