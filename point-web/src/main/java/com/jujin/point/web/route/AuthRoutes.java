package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.*;
import com.jujin.point.service.*;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

import java.util.*;

public class AuthRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/auth",
            Route.post("/signup", ctx -> {
                var req = ctx.bodyAsJson(CreateUserRequest.class);
                var user = userSvc().signUp(req);
                var token = authSvc().createToken(user.getId(), user.getNickname(),
                    user.getAvatar() != null ? user.getAvatar() : "", null);
                ctx.sendJson(201, ApiResponse.ok(Map.of("token", token, "userId", user.getId(), "nickname", user.getNickname())));
            }),
            Route.post("/signin", ctx -> {
                var req = ctx.bodyAsJson(SignInRequest.class);
                var user = userSvc().signIn(req.loginName(), req.password());
                var token = authSvc().createToken(user.getId(), user.getNickname(),
                    user.getAvatar() != null ? user.getAvatar() : "", null);
                ctx.sendJson(200, ApiResponse.ok(Map.of("token", token, "userId", user.getId(),
                    "nickname", user.getNickname(), "avatar", user.getAvatar() != null ? user.getAvatar() : "")));
            }),
            Route.post("/signout", ctx -> ctx.sendJson(200, ApiResponse.ok())),

            // GitHub OAuth
            Route.get("/github/authorize", ctx -> {
                var provider = AppContext.get(GitHubOAuthProvider.class);
                ctx.sendJson(200, ApiResponse.ok(Map.of("url", provider.authorizeUrl(UUID.randomUUID().toString()))));
            }),
            Route.get("/github/callback", ctx -> {
                String code = ctx.queryParam("code");
                if (code == null) { ctx.sendJson(400, ApiResponse.error("缺少code")); return; }
                var info = AppContext.get(GitHubOAuthProvider.class).handleCallback(code);
                if (info == null) { ctx.sendJson(400, ApiResponse.error("OAuth授权失败")); return; }
                var runtime = handleOAuthLogin("github", info);
                ctx.sendJson(200, ApiResponse.ok(runtime));
            }),
            Route.post("/github/bind", ctx -> {
                var user = AuthFilter.requireUser();
                String code = ctx.queryParam("code");
                if (code == null) { ctx.sendJson(400, ApiResponse.error("缺少code")); return; }
                var info = AppContext.get(GitHubOAuthProvider.class).handleCallback(code);
                if (info == null) { ctx.sendJson(400, ApiResponse.error("OAuth授权失败")); return; }
                bindThirdUser(user.userId(), "github", info);
                ctx.sendJson(200, ApiResponse.ok(Map.of("bound", true)));
            }),
            Route.post("/github/unbind", ctx -> {
                var user = AuthFilter.requireUser();
                AppContext.get(Database.class).execute("DELETE FROM bbs_third_user WHERE user_id=? AND third_type=?", user.userId(), "github");
                ctx.sendJson(200, ApiResponse.ok());
            })
        );
    }

    private static Map<String, Object> handleOAuthLogin(String provider, OAuthProvider.OAuthUserInfo info) {
        var db = AppContext.get(Database.class);
        var orm = Orm.of(db);
        var authSvc = AppContext.get(AuthService.class);
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
            var r = db.execute("INSERT INTO bbs_user (nickname, avatar, score, exp, level, status, topic_count, comment_count, follow_count, fans_count, forbidden_end_time, create_time, update_time) VALUES (?,?,0,0,1,1,0,0,0,0,0,?,?)", info.nickname(), info.avatar(), now, now);
            if (r.hasKey()) user.setId(r.longKey());
            var tu = new ThirdUser(); tu.setUserId(user.getId()); tu.setOpenId(info.openId()); tu.setThirdType(provider);
            tu.setNickname(info.nickname()); tu.setAvatar(info.avatar()); tu.setCreateTime(now); tu.setUpdateTime(now);
            orm.insert(tu);
        }
        String token = authSvc.createToken(user.getId(), user.getNickname(), user.getAvatar() != null ? user.getAvatar() : "", null);
        return Map.of("token", token, "userId", user.getId(), "nickname", user.getNickname());
    }

    private static void bindThirdUser(long userId, String provider, OAuthProvider.OAuthUserInfo info) {
        var db = AppContext.get(Database.class);
        if (db.query("SELECT 1 FROM bbs_third_user WHERE open_id=? AND third_type=?", info.openId(), provider).list(com.jujin.freeway.db.Row.class).isEmpty()) {
            var tu = new ThirdUser(); long now = System.currentTimeMillis();
            tu.setUserId(userId); tu.setOpenId(info.openId()); tu.setThirdType(provider);
            tu.setNickname(info.nickname()); tu.setAvatar(info.avatar());
            tu.setCreateTime(now); tu.setUpdateTime(now);
            Orm.of(db).insert(tu);
        }
    }

    private static UserService userSvc() { return AppContext.get(UserService.class); }
    private static AuthService authSvc() { return AppContext.get(AuthService.class); }
}
