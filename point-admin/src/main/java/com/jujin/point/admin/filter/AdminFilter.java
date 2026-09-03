package com.jujin.point.admin.filter;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteHandler;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.domain.dto.ApiResponse;

/**
 * Admin authorization filter — must be placed AFTER AuthFilter.
 * Only allows users with admin or owner roles.
 *
 * Identity comes from the AuthApi CallBus consumer (served by the web
 * layer's AuthRpc) — no compile-time dependency on point-web.
 */
public class AdminFilter implements HttpFilter {

    private final AuthApi authApi;

    public AdminFilter(AuthApi authApi) {
        this.authApi = authApi;
    }

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        // Only enforce for admin paths
        if (!ctx.path().startsWith("/api/admin")) {
            next.handle(ctx);
            return;
        }
        if (!authApi.isAdmin()) {
            ctx.sendJson(403, ApiResponse.error(403, "需要管理员权限"));
            return;
        }
        next.handle(ctx);
    }
}
