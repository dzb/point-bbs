package com.jujin.point.web;

import com.jujin.freeway.http.filter.ErrorHandler;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.staticfile.StaticResourceMount;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.ServiceException;
import com.jujin.point.web.filter.AuthException;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.point.web.filter.SecurityHeadersFilter;
import com.jujin.point.web.filter.SpaFilter;
import com.jujin.point.web.route.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web module — serves the SPA and REST API routes.
 *
 * freeway 1.3.8: filters use canonical auto-IDs via add(Class) instead of
 * manual id strings. AuthFilter receives AuthService + PermissionService via DI.
 * ResponseEnricher is bound as a singleton with Database injection.
 */
public class WebModule implements ModuleEx {
    private static final Logger log = LoggerFactory.getLogger(WebModule.class);

    @Override
    public void bind(Binder binder) {
        // Response enricher — injected with Database, used by route handlers
        binder.bind(ResponseEnricher.class).to(ResponseEnricher.class);
        // Auth rate limiter (signin/signup brute-force guard)
        binder.bind(LoginRateLimiter.class).to(new LoginRateLimiter(10, 10 * 60 * 1000));

        // WebSocket push notifications
        binder.bind(NotificationHub.class).to(new NotificationHub());
        binder
            .contribute(com.jujin.freeway.http.websocket.WebSocketGroup.class)
            .add(
                com.jujin.freeway.http.websocket.WebSocketGroup.of(
                    "/ws",
                    com.jujin.freeway.http.websocket.WebSocketRoute.of(
                        "/notify",
                        new NotificationWsEndpoint()
                    )
                )
            );
        // Serve hashed JS/CSS assets with immutable caching (Vite content-hash filenames)
        binder
            .contribute(StaticResourceMount.class)
            .add(
                StaticResourceMount.classpath("/assets", "static/assets")
                    .immutable(true)
                    .cacheMaxAgeSeconds(31536000)
            ); // 1 year

        // Serve root-level static files (favicon, etc.) — fallthrough for SPA routes
        binder
            .contribute(StaticResourceMount.class)
            .add(
                StaticResourceMount.classpath("/", "static")
                    .fallthrough(true)
                    .cacheMaxAgeSeconds(86400)
            ); // 1 day

        // SPA frontend fallthrough filter (skips /api paths)
        binder.contribute(HttpFilter.class).add("spa-filter", new SpaFilter());

        // Security headers on every response. Must run BEFORE spa-filter:
        // SpaFilter serves index.html without calling next.handle(), so any
        // filter after it never sees SPA routes.
        binder
            .contribute(HttpFilter.class)
            .add("security-headers", new SecurityHeadersFilter())
            .before("spa-filter");

        // Auth filter — bound as a singleton then contributed (so container can resolve its DI)
        binder.bind(AuthFilter.class).to(AuthFilter.class);
        binder
            .contribute(HttpFilter.class)
            .add(AuthFilter.class)
            .after("security-headers");

        // Serve session identity over the CallBus ("auth.*" topics) so other
        // modules (admin) can ask who is calling without importing point-web.
        binder
            .contribute(com.jujin.freeway.ioc.RuntimeHook.class)
            .add(
                "auth-rpc",
                new com.jujin.freeway.ioc.RuntimeHook() {
                    @Override
                    public void start(com.jujin.freeway.ioc.Container container) {
                        container.get(com.jujin.freeway.ioc.CallBus.class)
                            .register("auth", new AuthRpc());
                    }

                    @Override
                    public void stop(com.jujin.freeway.ioc.Container container) {}
                }
            )
            .before("freeway.http.server");

        // Exception mappers
        binder.contribute(ErrorHandler.class).add((resp, ex) -> {
            if (ex instanceof AuthException ae) {
                resp.sendJson(
                    ae.statusCode(),
                    ApiResponse.error(ae.statusCode(), ae.getMessage())
                );
                return true;
            }
            if (ex instanceof ServiceException se) {
                resp.sendJson(400, ApiResponse.error(se.getMessage()));
                return true;
            }
            if (ex instanceof com.jujin.freeway.http.ValidationException ve) {
                var details = ve
                    .result()
                    .getErrors()
                    .stream()
                    .map(e -> e.field() + ": " + e.message())
                    .collect(java.util.stream.Collectors.joining("; "));
                resp.sendJson(400, ApiResponse.error("参数校验失败: " + details));
                return true;
            }
            log.error(
                "Unhandled exception {}: {}",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
            );
            resp.sendJson(500, ApiResponse.error(500, "服务器内部错误"));
            return true;
        });

        // Push "refresh" pings over WebSocket when a notification row is
        // actually written. MessageService publishes NotificationSentEvent
        // once per persisted message — the single push signal. Recipient
        // resolution lives entirely in the notification flow; the SPA
        // re-fetches the unread count on ping (15s polling remains as
        // fallback).
        binder
            .contribute(com.jujin.freeway.ioc.EventSubscriber.class)
            .add(
                "ws-ping",
                com.jujin.freeway.ioc.EventSubscriber.of(
                    com.jujin.point.domain.event.NotificationSentEvent.class,
                    e ->
                        com.jujin.point.domain.AppContext
                            .get(NotificationHub.class)
                            .pingUnread(e.toUserId())
                )
            );

        // API route groups
        binder.contribute(RouteGroup.class).add(TopicRoutes.routes());
        binder.contribute(RouteGroup.class).add(ArticleRoutes.routes());
        binder.contribute(RouteGroup.class).add(UserRoutes.routes());
        binder.contribute(RouteGroup.class).add(AuthRoutes.routes());
        binder.contribute(RouteGroup.class).add(CategoryRoutes.routes());
        binder.contribute(RouteGroup.class).add(UploadRoutes.routes());
    }
}
