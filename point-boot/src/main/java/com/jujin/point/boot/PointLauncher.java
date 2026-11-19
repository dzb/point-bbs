package com.jujin.point.boot;

import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.cloud.CloudModule;
import com.jujin.freeway.cloud.discovery.StaticModule;
import com.jujin.freeway.ioc.symbol.SymbolSource;
import com.jujin.point.boot.cloud.AuthRpcExportModule;
import com.jujin.point.boot.cloud.CloudHealthModule;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.logging.LogManager;

/**
 * Shared startup for both deployment shapes.
 *
 * <p>{@link PointApp} and {@link PointCloudApp} keep their own {@code main}
 * because the process command line is the deployment's identity — {@code
 * point.sh} reads {@code PointCloudApp} out of {@code ps} to detect cloud mode.
 * What they no longer duplicate is the startup choreography: logging setup,
 * banner, port report and staying alive.</p>
 */
final class PointLauncher {

    private PointLauncher() {}

    static void run(String[] args, boolean cloud) throws Exception {
        initLogging();
        printBanner();

        var launcher = FreewayApp
            .create(PointModules.base())
            .name("point")
            // Explicit composition: nothing is discovered and installed twice.
            .autoDiscovery(false);
        if (cloud) {
            launcher = launcher
                .add(new AuthRpcExportModule())
                .add(new CloudHealthModule())
                .add(CloudModule.class)
                .add(new StaticModule());
        }
        var runtime = launcher.args(args).start();

        var port = runtime.get(SymbolSource.class).resolve("freeway.http.server.port", null);
        System.out.println(
            (cloud ? "point (cloud shape)" : "point") +
            " running on http://localhost:" + (port != null ? port : 8082) +
            (cloud ? " — auth exported as /rpc/auth/validateToken" : "")
        );
        System.out.println("Press Ctrl+C to stop.");

        // Keep JVM alive — freeway HTTP acceptor threads are daemon.
        // Shutdown is handled by the JVM shutdown hook registered by FreewayApp.
        new CountDownLatch(1).await();
    }

    private static void initLogging() throws Exception {
        // JUL does not auto-create the directory its file handler writes to.
        Files.createDirectories(Path.of("logs"));
        var logConfig = PointLauncher.class.getClassLoader().getResource("logging.properties");
        if (logConfig != null) {
            try (var stream = logConfig.openStream()) {
                LogManager.getLogManager().readConfiguration(stream);
            }
        }
    }

    private static void printBanner() {
        System.out.println(
            """
             ____  ____  ____  _  _  ____
            ||P ||||O ||||I ||||N ||||T ||
            ||__||||__||||__||||__||||__||
            |/__\\\\||/__\\\\||/__\\\\||/__\\\\||/__\\\\|
            point v%s -- powered by freeway %s + JDK %s
            """.formatted(
                BuildInfo.pointVersion(),
                BuildInfo.freewayVersion(),
                Runtime.version().feature()
            )
        );
    }
}
