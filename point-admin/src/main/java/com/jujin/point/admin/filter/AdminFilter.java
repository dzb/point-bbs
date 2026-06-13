package com.jujin.point.admin.filter;

import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.HttpFilter;
import com.jujin.freeway.http.RouteHandler;

/**
 * Admin authorization filter — must be placed AFTER AuthFilter.
 * Only allows users with admin or owner roles.
 */
public class AdminFilter implements HttpFilter {

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        // Only enforce for admin paths
        if (!ctx.path().startsWith("/api/admin")) {
            next.handle(ctx);
            return;
        }
        var user = AuthFilter.currentUser();
        if (user == null || !user.isAdmin()) {
            ctx.sendJson(403, ApiResponse.error(403, "需要管理员权限"));
            return;
        }
        next.handle(ctx);
    }
}
