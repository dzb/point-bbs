package com.jujin.point.web.filter;

import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;
import com.jujin.point.service.PermissionService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteHandler;

/**
 * Authentication filter — extracts JWT token and resolves CurrentUser via ScopedValue.
 *
 * AuthService and PermissionService are injected by the container.
 */
public class AuthFilter implements HttpFilter {
    private static final ScopedValue<CurrentUser> CURRENT_USER = ScopedValue.newInstance();

    private final AuthService authService;
    private final PermissionService permissionService;

    public AuthFilter(AuthService authService, PermissionService permissionService) {
        this.authService = authService;
        this.permissionService = permissionService;
    }

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
            user = authService.validateToken(token).orElse(null);
            if (user != null) {
                var permCodes = permissionService.getUserPermissionCodes(user.userId());
                user = new CurrentUser(user.userId(), user.nickname(), user.avatar(), permCodes);
            }
        }
        ScopedValue.where(CURRENT_USER, user).call(() -> {
            next.handle(ctx);
            return null;
        });
    }

    private String extractToken(HttpContext ctx) {
        var auth = ctx.header("Authorization").orElse(null);
        if (auth != null) {
            if (auth.startsWith("Bearer ")) return auth.substring(7);
            return auth;
        }
        return ctx.queryParam("token").orElse(null);
    }
}
