package com.jujin.point.web;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.HttpFilter;
import com.jujin.freeway.http.RouteHandler;

/**
 * Simple CORS filter allowing all origins for development.
 */
public class CorsFilter implements HttpFilter {
    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        ctx.headerSet("Access-Control-Allow-Origin", "*");
        ctx.headerSet("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        ctx.headerSet("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Token");
        ctx.headerSet("Access-Control-Max-Age", "86400");

        if ("OPTIONS".equalsIgnoreCase(ctx.method())) {
            ctx.send(204, "");
            return;
        }
        next.handle(ctx);
    }
}
