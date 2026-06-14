package com.jujin.point.web.filter;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.HttpFilter;
import com.jujin.freeway.http.RouteHandler;

import java.io.InputStream;

/**
 * Serves the SPA frontend from classpath:/static for non-API GET requests.
 * StaticResourceMount is not suitable here — it matches before routes and would
 * consume API requests when mounted at "/".
 */
public class SpaFilter implements HttpFilter {

    private static final String STATIC_ROOT = "/static";

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        if (!"GET".equalsIgnoreCase(ctx.method()) || ctx.path().startsWith("/api")) {
            next.handle(ctx);
            return;
        }

        var resourcePath = STATIC_ROOT + (ctx.path().equals("/") ? "/index.html" : ctx.path());
        var stream = SpaFilter.class.getResourceAsStream(resourcePath);

        if (stream == null) {
            stream = SpaFilter.class.getResourceAsStream(STATIC_ROOT + "/index.html");
        }

        if (stream == null) {
            next.handle(ctx);
            return;
        }

        var bytes = stream.readAllBytes();
        stream.close();
        ctx.headerSet("Content-Type", guessMime(resourcePath));
        ctx.headerSet("Cache-Control", "public, max-age=3600");
        ctx.status(200).output(bytes);
    }

    private static String guessMime(String path) {
        var lower = path.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=utf-8";
        if (lower.endsWith(".css"))  return "text/css; charset=utf-8";
        if (lower.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".ico"))  return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }
}
