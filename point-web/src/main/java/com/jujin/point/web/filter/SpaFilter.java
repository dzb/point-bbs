package com.jujin.point.web.filter;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteHandler;

import java.io.InputStream;

/**
 * SPA fallback: serves index.html for client-side navigation routes.
 *
 * Static file requests (assets, favicon, etc.) are handled natively by
 * freeway's {@link com.jujin.freeway.http.StaticResourceMount} with proper
 * ETag/304 caching — this filter only handles the SPA entry-point.
 */
public class SpaFilter implements HttpFilter {

    private static final String INDEX_PATH = "/static/index.html";

    private byte[] indexHtml;
    private volatile boolean loaded;

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        if (!"GET".equalsIgnoreCase(ctx.method()) || ctx.path().startsWith("/api")) {
            next.handle(ctx);
            return;
        }

        // Paths with a file extension are static assets — let StaticResourceMount handle them
        var path = ctx.path();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && path.indexOf('.', lastSlash) > lastSlash) {
            next.handle(ctx);
            return;
        }

        // SPA navigation route — serve index.html
        var bytes = getIndexHtml();
        if (bytes == null) {
            next.handle(ctx);
            return;
        }
        ctx.headerSet("Content-Type", "text/html; charset=utf-8");
        ctx.headerSet("Cache-Control", "no-cache");
        ctx.status(200).output(bytes);
    }

    private byte[] getIndexHtml() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    try (var stream = SpaFilter.class.getResourceAsStream(INDEX_PATH)) {
                        if (stream != null) {
                            indexHtml = stream.readAllBytes();
                        }
                    } catch (Exception ignored) {
                    }
                    loaded = true;
                }
            }
        }
        return indexHtml;
    }
}
