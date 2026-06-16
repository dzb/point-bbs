package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.service.*;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

import java.util.*;

public class UserRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/users",
            Route.get("/current", ctx -> {
                var user = AuthFilter.currentUser();
                if (user == null) { ctx.sendJson(200, ApiResponse.ok(null)); return; }
                var full = userSvc().findById(user.userId());
                full.ifPresent(u -> u.setPassword(null));
                ctx.sendJson(200, ApiResponse.ok(full.orElse(null)));
            }),
            Route.get("/{id}", ctx -> {
                var u = userSvc().findById(ctx.pathVar("id", Long.class));
                u.ifPresent(x -> x.setPassword(null));
                ctx.sendJson(200, ApiResponse.ok(u.orElse(null)));
            }),
            Route.post("/edit/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                long targetId = ctx.pathVar("id", Long.class);
                if (user.userId() != targetId) {
                    ctx.sendJson(403, ApiResponse.error(403, "只能编辑自己的资料"));
                    return;
                }
                userSvc().updateUser(targetId, ctx.bodyAsJson(UpdateUserRequest.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/topics", ctx -> {
                long uid = ctx.pathVar("id", Long.class);
                int page = intParam(ctx, "page", 1);
                var r = topicSvc().getUserTopics(uid, PageRequest.of(page, 20));
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichTopics(r.items())));
            }),
            Route.get("/{id}/articles", ctx -> {
                long uid = ctx.pathVar("id", Long.class);
                int page = intParam(ctx, "page", 1);
                var a = articleSvc().getByUser(uid, page, 20);
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichArticles(a)));
            }),
            Route.get("/{id}/messages", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
                int page = intParam(ctx, "page", 1);
                ctx.sendJson(200, ApiResponse.ok(msgSvc().getUserMessages(uid, page, 20)));
            }),
            Route.get("/{id}/messages/unread", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
                ctx.sendJson(200, ApiResponse.ok(Map.of("count", msgSvc().unreadCount(uid))));
            }),
            Route.post("/{id}/messages/read", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能操作自己的消息")); return; }
                var req = ctx.bodyAsJson(Map.class);
                @SuppressWarnings("unchecked")
                var ids = ((List<Number>) req.get("ids")).stream().map(Number::longValue).toList();
                msgSvc().markRead(uid, ids);
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/follow", ctx -> {
                var user = AuthFilter.requireUser();
                followSvc().follow(user.userId(), ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/unfollow", ctx -> {
                var user = AuthFilter.requireUser();
                followSvc().unfollow(user.userId(), ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/follow/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean f = user != null && followSvc().isFollowing(user.userId(), ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok(Map.of("following", f)));
            }),
            Route.get("/{id}/followers", ctx -> {
                int page = intParam(ctx, "page", 1);
                ctx.sendJson(200, ApiResponse.ok(followSvc().getFollowers(ctx.pathVar("id", Long.class), page, 20)));
            }),
            Route.get("/{id}/following", ctx -> {
                int page = intParam(ctx, "page", 1);
                ctx.sendJson(200, ApiResponse.ok(followSvc().getFollowing(ctx.pathVar("id", Long.class), page, 20)));
            }),
            Route.get("/{id}/favorites", ctx -> {
                int page = intParam(ctx, "page", 1);
                ctx.sendJson(200, ApiResponse.ok(favSvc().getUserFavorites(ctx.pathVar("id", Long.class), page, 20)));
            })
        );
    }

    private static UserService userSvc() { return AppContext.get(UserService.class); }
    private static TopicService topicSvc() { return AppContext.get(TopicService.class); }
    private static ArticleService articleSvc() { return AppContext.get(ArticleService.class); }
    private static MessageService msgSvc() { return AppContext.get(MessageService.class); }
    private static UserFollowService followSvc() { return AppContext.get(UserFollowService.class); }
    private static FavoriteService favSvc() { return AppContext.get(FavoriteService.class); }

    private static int intParam(HttpContext ctx, String name, int defaultVal) {
        Integer v = ctx.queryParam(name, Integer.class);
        return v != null ? v : defaultVal;
    }
}
