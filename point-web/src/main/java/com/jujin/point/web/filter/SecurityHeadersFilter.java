package com.jujin.point.web.filter;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteHandler;

/**
 * Adds baseline security headers to every response:
 * CSP (self-only, inline styles allowed for Vuetify), frame denial,
 * no-sniff, and a strict referrer policy.
 */
public class SecurityHeadersFilter implements HttpFilter {

    private static final String CSP =
        "default-src 'self'; " +
        "script-src 'self'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self' data:; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'; " +
        "base-uri 'self'; " +
        "form-action 'self'";

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        ctx.setHeader("Content-Security-Policy", CSP);
        ctx.setHeader("X-Frame-Options", "DENY");
        ctx.setHeader("X-Content-Type-Options", "nosniff");
        ctx.setHeader("Referrer-Policy", "same-origin");
        next.handle(ctx);
    }
}
