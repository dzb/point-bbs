package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.http.HttpModule;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Regression: the HTTP listener must not accept requests before seeding is
 * done.
 *
 * <p>The listener binds inside its own runtime hook. When the dev seed ran
 * after it, a probe could see {@code /healthz} answer 200 while topics and
 * users were still being written — CI's smoke sweep hit that window and read
 * {@code /api/topics/1} as 404. {@link SeedProfileTest} cannot catch this: it
 * inspects the database after {@code start()} returns, when every hook has
 * finished.</p>
 *
 * <p>This test therefore starts the application on another thread and polls
 * exactly like a probe, then issues the first data request with no sleep and
 * no retry — the request that used to race the seed.</p>
 */
class StartupOrderingTest {

    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(60);

    private volatile AppRuntime app;

    @AfterEach
    void stop() throws Exception {
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
    void seededDataExistsTheMomentHealthFirstAnswers() throws Exception {
        int port = freePort();
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, String.valueOf(port));
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:point_startup_order;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "startup-order-secret-123456789012");

        var startupFailure = new AtomicReference<Throwable>();
        var started = new CountDownLatch(1);
        Thread.ofVirtual().name("point-startup").start(() -> {
            try {
                app = FreewayApp
                    .create(PointModules.base())
                    .name("point-startup-order")
                    .autoDiscovery(false)
                    .args("--F2.profile=dev")
                    .start();
            } catch (Throwable failure) {
                startupFailure.set(failure);
            } finally {
                started.countDown();
            }
        });

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        HttpResponse<String> health = pollHealth(client, port, startupFailure);
        assertNull(startupFailure.get(), () -> "application failed to start: " + startupFailure.get());
        assertNotNull(health, "the server never became healthy within " + HEALTH_TIMEOUT);

        // No sleep, no retry: the listener has just started answering.
        var topic = get(client, port, "/api/topics/1");
        assertEquals(200, topic.statusCode(),
            "topic 1 answered " + topic.statusCode() + " — the listener is up before the seed finished");
        var user = get(client, port, "/api/users/1");
        assertEquals(200, user.statusCode(),
            "user 1 answered " + user.statusCode() + " — the listener is up before the seed finished");

        // Let the startup thread finish so @AfterEach closes a fully built
        // runtime (and any startup failure that only surfaced later is seen).
        started.await(30, TimeUnit.SECONDS);
        assertNull(startupFailure.get(), () -> "application failed to start: " + startupFailure.get());
    }

    private static HttpResponse<String> pollHealth(
        HttpClient client,
        int port,
        AtomicReference<Throwable> startupFailure
    ) throws Exception {
        long deadline = System.nanoTime() + HEALTH_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (startupFailure.get() != null) return null;
            try {
                var response = get(client, port, "/healthz");
                if (response.statusCode() == 200) return response;
            } catch (IOException notListeningYet) {
                // The listener has not bound yet; keep polling like a probe.
            }
            Thread.sleep(5);
        }
        return null;
    }

    private static HttpResponse<String> get(HttpClient client, int port, String path)
        throws IOException, InterruptedException {
        return client.send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static int freePort() throws IOException {
        try (var socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
