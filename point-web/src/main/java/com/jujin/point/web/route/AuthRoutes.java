package com.jujin.point.web.route;

import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.*;
import com.jujin.point.service.*;
import com.jujin.point.web.LoginRateLimiter;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.http.HttpContext;
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
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, CreateUserRequest.class);
            if (!rateLimit(limiter, ctx, "signup")) return;
            var user = userSvc.signUp(req);
            var token = authSvc.createToken(user.getId(), user.getNickname(),
                user.getAvatar() != null ? user.getAvatar() : "", null);
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token, 7 * 86400));
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
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, SignInRequest.class);
            if (!rateLimit(limiter, ctx, "signin")) return;
            var user = userSvc.signIn(req.loginName(), req.password());
            var token = authSvc.createToken(user.getId(), user.getNickname(),
                user.getAvatar() != null ? user.getAvatar() : "", null);
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token, 7 * 86400));
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
        public void handle(HttpContext ctx) throws Exception {
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
        public void handle(HttpContext ctx) throws Exception {
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
        private final Database db;

        public GithubCallbackHandler(GitHubOAuthProvider provider, AuthService authSvc, Database db) {
            this.provider = provider;
            this.authSvc = authSvc;
            this.db = db;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
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
            var runtime = handleOAuthLogin("github", info);
            String token = (String) runtime.get("token");
            if (token == null) { ctx.sendJson(400, ApiResponse.error("登录失败")); return; }
            // Redirect to the configured SPA origin — never to the request's
            // Origin header (open-redirect via a spoofed Origin). The token
            // goes into the HttpOnly session cookie, not the URL.
            var spaOrigin = provider.spaOrigin();
            ctx.setHeader("Set-Cookie", authSvc.sessionCookie(token, 7 * 86400));
            ctx.setHeader("Location", spaOrigin + "/#/");
            ctx.setStatus(302).output("Redirecting...".getBytes(StandardCharsets.UTF_8));
        }

        /** Find or create the third-party binding and issue a session token. */
        private Map<String, Object> handleOAuthLogin(String provider, OAuthProvider.OAuthUserInfo info) {
            var orm = Orm.of(db);
            var existing = db.query("SELECT * FROM bbs_third_user WHERE open_id=? AND third_type=?", info.openId(), provider).one(ThirdUser.class);
            User user;
            if (existing.isPresent()) {
                user = orm.findById(User.class, existing.get().getUserId()).orElse(null);
                if (user == null) return Map.of("error", "用户不存在");
            } else {
                user = new User(); long now = System.currentTimeMillis();
                user.setNickname(info.nickname()); user.setAvatar(info.avatar());
                user.setStatus(1); user.setLevel(1);
                user.setCreateTime(now); user.setUpdateTime(now);
                var created = new User[1];
                created[0] = user;
                try {
                    db.transaction(() -> {
                        var r = db.execute("INSERT INTO bbs_user (nickname, avatar, score, exp, level, status, topic_count, comment_count, follow_count, fans_count, forbidden_end_time, create_time, update_time) VALUES (?,?,0,0,1,1,0,0,0,0,0,?,?)", info.nickname(), info.avatar(), now, now);
                        if (r.hasGeneratedKey()) created[0].setId(r.longKey());
                        var tu = new ThirdUser(); tu.setUserId(created[0].getId()); tu.setOpenId(info.openId()); tu.setThirdType(provider);
                        tu.setNickname(info.nickname()); tu.setAvatar(info.avatar()); tu.setCreateTime(now); tu.setUpdateTime(now);
                        orm.insert(tu);
                    });
                } catch (com.jujin.freeway.db.SqlException e) {
                    // Unique (open_id, third_type) index — a concurrent callback
                    // created the account; fall back to the existing binding.
                    var again = db.query("SELECT * FROM bbs_third_user WHERE open_id=? AND third_type=?", info.openId(), provider).one(ThirdUser.class);
                    if (again.isPresent()) {
                        user = orm.findById(User.class, again.get().getUserId()).orElse(null);
                        if (user == null) return Map.of("error", "用户不存在");
                    } else {
                        return Map.of("error", "OAuth登录失败");
                    }
                }
            }
            String token = authSvc.createToken(user.getId(), user.getNickname(), user.getAvatar() != null ? user.getAvatar() : "", null);
            return Map.of("token", token, "id", user.getId(), "nickname", user.getNickname());
        }
    }

    public static final class GithubBindHandler implements RouteHandler {
        private final GitHubOAuthProvider provider;
        private final Database db;

        public GithubBindHandler(GitHubOAuthProvider provider, Database db) {
            this.provider = provider;
            this.db = db;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            String code = ctx.queryParam("code").orElse(null);
            if (code == null) { ctx.sendJson(400, ApiResponse.error("缺少code")); return; }
            var info = provider.handleCallback(code);
            if (info == null) { ctx.sendJson(400, ApiResponse.error("OAuth授权失败")); return; }
            bindThirdUser(user.userId(), "github", info);
            ctx.sendJson(200, ApiResponse.ok(Map.of("bound", true)));
        }

        private void bindThirdUser(long userId, String provider, OAuthProvider.OAuthUserInfo info) {
            if (db.query("SELECT 1 FROM bbs_third_user WHERE open_id=? AND third_type=?", info.openId(), provider).list(Row.class).isEmpty()) {
                var tu = new ThirdUser(); long now = System.currentTimeMillis();
                tu.setUserId(userId); tu.setOpenId(info.openId()); tu.setThirdType(provider);
                tu.setNickname(info.nickname()); tu.setAvatar(info.avatar());
                tu.setCreateTime(now); tu.setUpdateTime(now);
                Orm.of(db).insert(tu);
            }
        }
    }

    public static final class GithubUnbindHandler implements RouteHandler {
        private final Database db;

        public GithubUnbindHandler(Database db) {
            this.db = db;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            db.execute("DELETE FROM bbs_third_user WHERE user_id=? AND third_type=?", user.userId(), "github");
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    // ──── shared helpers ────

    /** Extract the OAuth state cookie value from the request, or null. */
    private static String oauthStateCookie(HttpContext ctx) {
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
    private static boolean rateLimit(LoginRateLimiter limiter, HttpContext ctx, String action)
        throws java.io.IOException {
        var ip = ctx.remoteAddress() != null ? ctx.remoteAddress() : "unknown";
        if (!limiter.allow(ip + ":" + action)) {
            ctx.sendJson(429, ApiResponse.error(429, "操作过于频繁，请稍后再试"));
            return false;
        }
        return true;
    }
}
