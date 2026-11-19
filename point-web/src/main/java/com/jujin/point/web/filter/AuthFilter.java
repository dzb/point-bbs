package com.jujin.point.web.filter;

import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.service.AuthService;
import com.jujin.point.service.PermissionService;
import com.jujin.point.service.ServiceException;
import com.jujin.point.service.UserService;
import com.jujin.freeway.http.HttpExchange;
import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteHandler;
import com.jujin.freeway.ioc.Container;

/**
 * Authentication filter — extracts JWT token and resolves CurrentUser via ScopedValue.
 *
 * AuthService, PermissionService and UserService are injected by the container.
 */
public class AuthFilter implements HttpFilter {
    private static final ScopedValue<CurrentUser> CURRENT_USER = ScopedValue.newInstance();

    private final Container container;

    /**
     * freeway 2.0 constructs deferred contributions before the extension
     * stores are sealed; resolving the service graph here would realize
     * EventBus while its subscriber store is still publishing. The filter
     * therefore holds the container and resolves services on the request
     * thread, after startup.
     */
    public AuthFilter(Container container) {
        this.container = container;
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
    public void doFilter(HttpExchange ctx, RouteHandler next) throws Exception {
        var authService = container.get(AuthService.class);
        var permissionService = container.get(PermissionService.class);
        var userService = container.get(UserService.class);
        var token = extractToken(ctx);
        CurrentUser user = null;
        if (token != null) {
            user = authService.validateToken(token).orElse(null);
            if (user != null) {
                // Mute/ban gate on mutations only — muted users keep read
                // access. Cached 60s in UserService; a fresh mute bites at
                // once because setForbiddenEndTime invalidates the entry.
                if (!"GET".equalsIgnoreCase(ctx.method())) {
                    try {
                        userService.ensureUsable(user.userId());
                    } catch (ServiceException se) {
                        throw new AuthException(se.getMessage(), 403);
                    }
                }
                var permCodes = permissionService.getUserPermissionCodes(user.userId());
                user = new CurrentUser(user.userId(), user.nickname(), user.avatar(), permCodes);
            }
        }
        ScopedValue.where(CURRENT_USER, user).call(() -> {
            next.handle(ctx);
            return null;
        });
    }

    private String extractToken(HttpExchange ctx) {
        var auth = ctx.header("Authorization").orElse(null);
        if (auth != null) {
            if (auth.startsWith("Bearer ")) return auth.substring(7);
            return auth;
        }
        // HttpOnly session cookie (SameSite=Lax) — the primary session carrier
        var cookie = AuthService.tokenFromCookie(ctx.header("Cookie").orElse(null));
        if (cookie != null) return cookie;
        // No ?token= fallback: a URL carrying a credential lands in access
        // logs, Referer headers, proxy logs and browser history. The SPA
        // sends the cookie; scripts use Authorization.
        return null;
    }
}
