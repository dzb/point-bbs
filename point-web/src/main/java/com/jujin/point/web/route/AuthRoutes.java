package com.jujin.point.web.route;

import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.*;
import com.jujin.point.service.*;
import com.jujin.point.web.LoginRateLimiter;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.HttpExchange;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class AuthRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/auth",
            Route.post("/signup", SignupHandler.class),
            Route.post("/signin", SigninHandler.class),
            Route.post("/signout", SignoutHandler.class),

            // GitHub OAuth
            Route.get("/github/authorize", GithubAuthorizeHandler.class),
            Route.get("/github/callback", GithubCallbackHandler.class),
            Route.post("/github/bind", GithubBindHandler.class),
            Route.post("/github/unbind", GithubUnbindHandler.class)
        );
    }

    // ──── signup / signin / signout ────

    public static final class SignupHandler implements RouteHandler {
        private final UserService userSvc;
        private final AuthService authSvc;
        private final LoginRateLimiter limiter;

        public SignupHandler(UserService userSvc, AuthService authSvc, LoginRateLimiter limiter) {
            this.userSvc = userSvc;
            this.authSvc = authSvc;
            this.limiter = limiter;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, CreateUserRequest.class);
            if (!rateLimit(limiter, ctx, "signup")) return;
            var user = userSvc.signUp(req);
            var token = authSvc.createToken(user.getId(), user.getNickname(),
                user.getAvatar() != null ? user.getAvatar() : "", null);
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token));
            ctx.sendJson(201, ApiResponse.ok(Map.of("token", token, "id", user.getId(),
                "nickname", user.getNickname(), "avatar", user.getAvatar() != null ? user.getAvatar() : "")));
        }
    }

    public static final class SigninHandler implements RouteHandler {
        private final UserService userSvc;
        private final AuthService authSvc;
        private final LoginRateLimiter limiter;

        public SigninHandler(UserService userSvc, AuthService authSvc, LoginRateLimiter limiter) {
            this.userSvc = userSvc;
            this.authSvc = authSvc;
            this.limiter = limiter;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, SignInRequest.class);
            if (!rateLimit(limiter, ctx, "signin")) return;
            var user = userSvc.signIn(req.loginName(), req.password());
            var token = authSvc.createToken(user.getId(), user.getNickname(),
                user.getAvatar() != null ? user.getAvatar() : "", null);
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token));
            ctx.sendJson(200, ApiResponse.ok(Map.of("token", token, "id", user.getId(),
                "nickname", user.getNickname(), "avatar", user.getAvatar() != null ? user.getAvatar() : "")));
        }
    }

    public static final class SignoutHandler implements RouteHandler {
        private final AuthService authSvc;

        public SignoutHandler(AuthService authSvc) {
            this.authSvc = authSvc;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            ctx.setHeader("Set-Cookie", authSvc.clearSessionCookie());
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    // ──── GitHub OAuth ────

    public static final class GithubAuthorizeHandler implements RouteHandler {
        private final GitHubOAuthProvider provider;

        public GithubAuthorizeHandler(GitHubOAuthProvider provider) {
            this.provider = provider;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            // Login-CSRF protection: the state is stored in an HttpOnly,
            // SameSite=Lax cookie (sent back on the top-level callback
            // navigation) and verified in the callback.
            String state = UUID.randomUUID().toString();
            ctx.setHeader(
                "Set-Cookie",
                "point_oauth_state=" + state +
                    "; Path=/api/auth/github/callback; HttpOnly; SameSite=Lax; Max-Age=600"
            );
            ctx.sendJson(200, ApiResponse.ok(Map.of("url", provider.authorizeUrl(state))));
        }
    }

    public static final class GithubCallbackHandler implements RouteHandler {
        private final GitHubOAuthProvider provider;
        private final AuthService authSvc;
        private final UserService userSvc;

        public GithubCallbackHandler(GitHubOAuthProvider provider, AuthService authSvc, UserService userSvc) {
            this.provider = provider;
            this.authSvc = authSvc;
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            String code = ctx.queryParam("code").orElse(null);
            if (code == null) { ctx.sendJson(400, ApiResponse.error("缺少code")); return; }
            String state = ctx.queryParam("state").orElse(null);
            String cookieState = oauthStateCookie(ctx);
            if (state == null || !state.equals(cookieState)) {
                ctx.sendJson(400, ApiResponse.error("OAuth state 校验失败，请重试"));
                return;
            }
            // Consume the state cookie regardless of the outcome
            ctx.setHeader(
                "Set-Cookie",
                "point_oauth_state=; Path=/api/auth/github/callback; HttpOnly; SameSite=Lax; Max-Age=0"
            );
            var info = provider.handleCallback(code);
            if (info == null) { ctx.sendJson(400, ApiResponse.error("OAuth授权失败")); return; }
            var user = userSvc
                .loginWithThirdParty("github", info.openId(), info.nickname(), info.avatar())
                .orElse(null);
            if (user == null) { ctx.sendJson(400, ApiResponse.error("登录失败")); return; }
            String token = authSvc.createToken(
                user.getId(),
                user.getNickname(),
                user.getAvatar() != null ? user.getAvatar() : "",
                null
            );
            // Redirect to the configured SPA origin — never to the request's
            // Origin header (open-redirect via a spoofed Origin). The token
            // goes into the HttpOnly session cookie, not the URL.
            var spaOrigin = provider.spaOrigin();
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token));
            ctx.setHeader("Location", spaOrigin + "/#/");
            ctx.setStatus(302).body("Redirecting...".getBytes(StandardCharsets.UTF_8));
        }
    }

    public static final class GithubBindHandler implements RouteHandler {
        private final GitHubOAuthProvider provider;
        private final UserService userSvc;

        public GithubBindHandler(GitHubOAuthProvider provider, UserService userSvc) {
            this.provider = provider;
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            var user = AuthFilter.requireUser();
            String code = ctx.queryParam("code").orElse(null);
            if (code == null) { ctx.sendJson(400, ApiResponse.error("缺少code")); return; }
            var info = provider.handleCallback(code);
            if (info == null) { ctx.sendJson(400, ApiResponse.error("OAuth授权失败")); return; }
            userSvc.bindThirdParty(user.userId(), "github", info.openId(), info.nickname(), info.avatar());
            ctx.sendJson(200, ApiResponse.ok(Map.of("bound", true)));
        }
    }

    public static final class GithubUnbindHandler implements RouteHandler {
        private final UserService userSvc;

        public GithubUnbindHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpExchange ctx) throws Exception {
            var user = AuthFilter.requireUser();
            userSvc.unbindThirdParty(user.userId(), "github");
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    // ──── shared helpers ────

    /** Extract the OAuth state cookie value from the request, or null. */
    private static String oauthStateCookie(HttpExchange ctx) {
        var cookie = ctx.header("Cookie").orElse(null);
        if (cookie == null) return null;
        for (String part : cookie.split(";")) {
            var kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("point_oauth_state")) {
                return kv[1];
            }
        }
        return null;
    }

    /** Apply auth rate limiting per client IP; responds 429 when exceeded. */
    private static boolean rateLimit(LoginRateLimiter limiter, HttpExchange ctx, String action)
        throws java.io.IOException {
        var ip = ctx.remoteAddress() != null ? ctx.remoteAddress() : "unknown";
        if (!limiter.allow(ip + ":" + action)) {
            ctx.sendJson(429, ApiResponse.error(429, "操作过于频繁，请稍后再试"));
            return false;
        }
        return true;
    }
}
