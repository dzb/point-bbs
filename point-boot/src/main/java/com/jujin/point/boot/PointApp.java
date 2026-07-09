package com.jujin.point.boot;

import com.jujin.freeway.boot.FreewayApp;

/**
 * point application entry point.
 */
public class PointApp {

    public static void main(String[] args) throws Exception {
        // Ensure log directory exists (JUL doesn't auto-create)
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("logs"));

        // Load JUL logging config — freeway uses java.util.logging via its SLF4J provider
        var logConfig = PointApp.class.getClassLoader().getResource("logging.properties");
        if (logConfig != null) {
            java.util.logging.LogManager.getLogManager().readConfiguration(logConfig.openStream());
        }

        System.out.println("""
             ____  ____  ____  _  _  ____
            ||P ||||O ||||I ||||N ||||T ||
            ||__||||__||||__||||__||||__||
            |/__\\\\||/__\\\\||/__\\\\||/__\\\\||/__\\\\|
            point v1.0.0 -- powered by freeway 1.3.2 + JDK %s
            """.formatted(Runtime.version().feature()));

        var runtime = FreewayApp.run(args, new PointModule());

        var port = runtime.config().get("freeway.web.server.port");
        System.out.println("point running on http://localhost:" + (port != null ? port : 8082));
        System.out.println("Press Ctrl+C to stop.");

        // Keep JVM alive — freeway HTTP acceptor threads are daemon.
        // Shutdown is handled by the JVM shutdown hook registered by FreewayApp.
        new java.util.concurrent.CountDownLatch(1).await();
    }
}
