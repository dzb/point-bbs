package com.jujin.point.web;

import com.jujin.freeway.http.filter.ExceptionMapper;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.staticfile.StaticResourceMount;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.ServiceException;
import com.jujin.point.web.filter.AuthException;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.point.web.filter.SpaFilter;
import com.jujin.point.web.route.*;

/**
 * Web module — serves the SPA and REST API routes.
 *
 * Freeway 1.3.2: filters use canonical auto-IDs via add(Class) instead of
 * manual id strings. AuthFilter receives AuthService + PermissionService via DI.
 * ResponseEnricher is bound as a singleton with Database injection.
 */
public class WebModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Response enricher — injected with Database, used by route handlers
        binder.bind(ResponseEnricher.class).to(ResponseEnricher.class);
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

        // Auth filter — bound as a singleton then contributed (so container can resolve its DI)
        binder.bind(AuthFilter.class).to(AuthFilter.class);
        binder
            .contribute(HttpFilter.class)
            .add(AuthFilter.class)
            .after("spa-filter");

        // Exception mappers
        binder.contribute(ExceptionMapper.class).add((ctx, ex) -> {
            if (ex instanceof AuthException ae) {
                ctx.sendJson(
                    ae.statusCode(),
                    ApiResponse.error(ae.statusCode(), ae.getMessage())
                );
                return true;
            }
            if (ex instanceof ServiceException se) {
                ctx.sendJson(400, ApiResponse.error(se.getMessage()));
                return true;
            }
            System.err.println(
                "=== Unhandled exception for " +
                    ctx.method() +
                    " " +
                    ctx.path() +
                    " ==="
            );
            ex.printStackTrace(System.err);
            ctx.sendJson(
                500,
                ApiResponse.error(
                    500,
                    "服务器内部错误: " + ex.getClass().getSimpleName()
                )
            );
            return true;
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
