package com.jujin.point.web;

import com.jujin.freeway.http.filter.ErrorMapper;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.staticfile.StaticResourceMount;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.point.web.filter.PointErrorMapper;
import com.jujin.point.web.filter.SecurityHeadersFilter;
import com.jujin.point.web.route.*;

/**
 * Web module — serves the SPA and REST API routes.
 *
 * Filters carry canonical contribution ids; AuthFilter resolves its service
 * graph per request (freeway 2.0 seals extensions after construction).
 * ResponseEnricher is bound as a singleton with Database injection.
 */
public class WebModule implements ModuleEx {
    /** Sliding-window policy for signin/signup: at most 10 attempts per 10 minutes. */
    private static final int LOGIN_MAX_ATTEMPTS = 10;
    private static final long LOGIN_WINDOW_MS = 10 * 60 * 1000;

    @Override
    public void bind(Binder binder) {
        // Response enricher — injected with Database, used by route handlers
        binder.bind(ResponseEnricher.class).to(ResponseEnricher.class);
        // Auth rate limiter (signin/signup brute-force guard)
        binder.bind(LoginRateLimiter.class)
            .to(c -> new LoginRateLimiter(LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW_MS));

        // WebSocket push notifications
        binder.bind(NotificationHub.class).to(c -> new NotificationHub());
        binder
            .contribute(com.jujin.freeway.http.websocket.WebSocketGroup.class)
            .add(
                com.jujin.freeway.http.websocket.WebSocketGroup.of(
                    "/ws",
                    com.jujin.freeway.http.websocket.WebSocketRoute.of(
                        "/notify",
                        NotificationWsEndpoint.class
                    )
                )
            );
        // Serve hashed JS/CSS assets with immutable caching (Vite content-hash filenames)
        binder
            .contribute(StaticResourceMount.class)
            .add(
                StaticResourceMount.classpath("/assets", "static/assets")
                    .withImmutable(true)
                    .withCacheMaxAge(java.time.Duration.ofSeconds(31536000))
            ); // 1 year

        // Serve root-level static files (favicon, etc.) and the SPA shell at
        // "/" (the mount's directory → index.html rule). Entry files must
        // revalidate every time (max-age=0 + ETag → cheap 304): index.html
        // references hashed bundles by name, so a cached index.html would pin
        // the browser to a stale frontend for a day.
        //
        // No withSpaFallback: the SPA uses hash routing, so "/" is the only
        // server-visible entry point, and a terminal fallback would answer
        // unknown /api (or /health) paths with 200 HTML. withRouteFallback
        // keeps a mount miss flowing into the route chain, whose own 404 stays
        // a JSON error. A filter cannot express this: it runs before route
        // dispatch, which is how the old spa-filter used to swallow
        // /health/live, /health/ready and the WebSocket upgrade.
        binder
            .contribute(StaticResourceMount.class)
            .add(
                StaticResourceMount.classpath("/", "static")
                    .withRouteFallback(true)
                    .withCacheMaxAge(java.time.Duration.ofSeconds(0))
            ); // revalidate (hashed /assets/* keep their own immutable 1y mount above)

        // Security headers on every response.
        binder
            .contribute(HttpFilter.class)
            .add("security-headers", new SecurityHeadersFilter());

        // Auth filter — built with the container, resolving its service graph
        // per request (freeway 2.0 seals extension stores after construction).
        binder
            .contribute(HttpFilter.class)
            .addDeferred("auth_filter@com.jujin.point.web.filter", c -> new AuthFilter(c))
            .after("security-headers");

        // Serve session identity as the AuthApi service so other modules
        // (admin) can ask who is calling without importing point-web — the
        // provider reads AuthFilter's ScopedValue-bound CurrentUser inline
        // on the requesting thread.
        binder.bind(AuthApi.class).to(AuthRpc.class);

        // Application error mapping. freeway 2.0's ErrorMapper returns the
        // response and the server writes it; framework request rejections
        // fall through to the built-in mapper.
        binder.contribute(ErrorMapper.class).add("point-errors", new PointErrorMapper());

        // Push "refresh" pings over WebSocket when a notification row is
        // actually written. MessageService publishes NotificationSentEvent
        // once per persisted message — the single push signal. Recipient
        // resolution lives entirely in the notification flow; the SPA
        // re-fetches the unread count on ping (15s polling remains as
        // fallback).
        binder
            .contribute(com.jujin.freeway.ioc.event.EventSubscriber.class)
            .addDeferred("ws-ping", c -> {
                NotificationHub hub = c.get(NotificationHub.class);
                return com.jujin.freeway.ioc.event.EventSubscriber.ofEvent(
                    com.jujin.point.domain.event.NotificationSentEvent.class,
                    e -> hub.pingUnread(e.toUserId())
                );
            });

        // API route groups
        binder.contribute(RouteGroup.class).add(TopicRoutes.routes());
        binder.contribute(RouteGroup.class).add(ArticleRoutes.routes());
        binder.contribute(RouteGroup.class).add(UserRoutes.routes());
        binder.contribute(RouteGroup.class).add(AuthRoutes.routes());
        binder.contribute(RouteGroup.class).add(CategoryRoutes.routes());
        binder.contribute(RouteGroup.class).add(UploadRoutes.routes());
    }
}
