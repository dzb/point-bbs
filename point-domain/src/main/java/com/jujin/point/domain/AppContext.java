package com.jujin.point.domain;

import com.jujin.freeway.ioc.Container;

/**
 * Static Container locator — a transitional bridge, not the target shape.
 *
 * <p>freeway's guidance reads this usage as the opposite of a recommendation:
 * "If you find yourself calling {@code AppContext.get(SomeService.class)}
 * inside a lambda handler, switch to a handler class — that is the signal
 * that you need constructor injection" (DEVELOPER-GUIDE, lambda vs handler
 * class). Handlers that depend on services become handler classes resolved
 * through the container at startup; this holder exists only while the route
 * classes are being migrated over and is deleted once they are.
 *
 * <p>Initialized by the {@code app-context-init} RuntimeHook before the HTTP
 * server accepts requests.
 */
public final class AppContext {
    private static volatile Container container;

    private AppContext() {}

    public static void init(Container c) {
        container = c;
    }

    public static Container container() {
        return container;
    }

    /** Convenience: resolve a service from the container. */
    public static <T> T get(Class<T> type) {
        return container.get(type);
    }

    /** Convenience: resolve a named service from the container. */
    public static <T> T get(Class<T> type, String id) {
        return container.get(type, id);
    }
}
