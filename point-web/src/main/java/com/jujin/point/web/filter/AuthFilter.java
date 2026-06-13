package com.jujin.point.web.filter;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;
import com.jujin.point.service.PermissionService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.HttpFilter;
import com.jujin.freeway.http.RouteHandler;

/**
 * Authentication filter — extracts JWT token and resolves CurrentUser via ScopedValue.
 */
public class AuthFilter implements HttpFilter {
    private static final ScopedValue<CurrentUser> CURRENT_USER = ScopedValue.newInstance();

    public static CurrentUser currentUser() {
        return CURRENT_USER.isBound() ? CURRENT_USER.get() : null;
    }

    public static CurrentUser requireUser() {
        var user = currentUser();
        if (user == null) throw new AuthException("请先登录");
        return user;
    }

    @Override
    public void doFilter(HttpContext ctx, RouteHandler next) throws Exception {
        var token = extractToken(ctx);
        CurrentUser user = null;
        if (token != null) {
            user = AppContext.get(AuthService.class).validateToken(token).orElse(null);
            if (user != null) {
                var permCodes = AppContext.get(PermissionService.class).getUserPermissionCodes(user.userId());
                user = new CurrentUser(user.userId(), user.nickname(), user.avatar(), permCodes);
            }
        }
        ScopedValue.where(CURRENT_USER, user).call(() -> {
            next.handle(ctx);
            return null;
        });
    }

    private String extractToken(HttpContext ctx) {
        var auth = ctx.header("Authorization");
        if (auth != null) {
            if (auth.startsWith("Bearer ")) return auth.substring(7);
            return auth;
        }
        return ctx.queryParam("token");
    }
}
