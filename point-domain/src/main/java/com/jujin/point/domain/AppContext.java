package com.jujin.point.domain;

import com.jujin.freeway.ioc.Container;

/**
 * Application context holder — provides Container access to route handlers.
 * Set during bootstrap via RuntimeHook before the HTTP server accepts requests.
 *
 * This is the recommended freeway pattern for making the Container available
 * to route handlers that need to resolve services at request time.
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
