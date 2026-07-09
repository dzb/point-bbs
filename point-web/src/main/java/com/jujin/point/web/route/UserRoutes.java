package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.service.*;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.util.*;

import static com.jujin.point.web.WebUtils.intParam;

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
            // User search for @mention autocomplete
            Route.get("/search", ctx -> {
                String q = ctx.queryParam("q").orElse(null);
                if (q == null || q.isBlank()) {
                    ctx.sendJson(200, ApiResponse.ok(List.of()));
                    return;
                }
                var users = userSvc().searchByPrefix(q.trim(), 10);
                users.forEach(u -> u.setPassword(null));
                ctx.sendJson(200, ApiResponse.ok(users));
            }),
            Route.get("/{id}", ctx -> {
                var u = userSvc().findById(resolveUserId(ctx));
                u.ifPresent(x -> x.setPassword(null));
                ctx.sendJson(200, ApiResponse.ok(u.orElse(null)));
            }),
            Route.post("/edit/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                long targetId = ctx.pathVar("id", Long.class).orElse(0L);
                if (user.userId() != targetId) {
                    ctx.sendJson(403, ApiResponse.error(403, "只能编辑自己的资料"));
                    return;
                }
                userSvc().updateUser(targetId, ctx.bodyAsJson(UpdateUserRequest.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/topics", ctx -> {
                long uid = resolveUserId(ctx);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var r = topicSvc().getUserTopics(uid, PageRequest.of(page, pageSize));
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", ResponseEnricher.enrichTopics(r.items()));
                resp.put("page", r.page());
                resp.put("pageSize", r.pageSize());
                resp.put("total", r.total());
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.get("/{id}/articles", ctx -> {
                long uid = resolveUserId(ctx);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var items = articleSvc().getByUser(uid, page, pageSize);
                var total = articleSvc().countByUser(uid);
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", ResponseEnricher.enrichArticles(items));
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", total);
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.get("/{id}/messages", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class).orElse(0L);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var items = msgSvc().getUserMessages(uid, page, pageSize);
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", items);
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", msgSvc().count(uid));
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.get("/{id}/messages/unread", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class).orElse(0L);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
                ctx.sendJson(200, ApiResponse.ok(Map.of("count", msgSvc().unreadCount(uid))));
            }),
            Route.post("/{id}/messages/read", ctx -> {
                var user = AuthFilter.requireUser();
                long uid = ctx.pathVar("id", Long.class).orElse(0L);
                if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能操作自己的消息")); return; }
                var req = ctx.bodyAsJson(Map.class);
                @SuppressWarnings("unchecked")
                var ids = ((List<Number>) req.get("ids")).stream().map(Number::longValue).toList();
                msgSvc().markRead(uid, ids);
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/follow", ctx -> {
                var user = AuthFilter.requireUser();
                followSvc().follow(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/unfollow", ctx -> {
                var user = AuthFilter.requireUser();
                followSvc().unfollow(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/follow/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean f = user != null && followSvc().isFollowing(user.userId(), resolveUserId(ctx));
                ctx.sendJson(200, ApiResponse.ok(Map.of("following", f)));
            }),
            Route.get("/{id}/followers", ctx -> {
                long uid = resolveUserId(ctx);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var items = followSvc().getFollowers(uid, page, pageSize);
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", items);
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", followSvc().countFollowers(uid));
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.get("/{id}/following", ctx -> {
                long uid = resolveUserId(ctx);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var items = followSvc().getFollowing(uid, page, pageSize);
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", items);
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", followSvc().countFollowing(uid));
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.get("/{id}/favorites", ctx -> {
                long uid = resolveUserId(ctx);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var items = favSvc().getUserFavorites(uid, page, pageSize);
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", items);
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", favSvc().count(uid));
                ctx.sendJson(200, ApiResponse.ok(resp));
            })
        );
    }

    /** Resolve a path variable to a user ID, accepting both numeric IDs and usernames. */
    private static long resolveUserId(HttpContext ctx) {
        var idStr = ctx.pathVar("id", String.class).orElse(null);
        try { return Long.parseLong(idStr); }
        catch (NumberFormatException e) {
            return AppContext.get(UserService.class).findByUsername(idStr)
                .map(com.jujin.point.domain.entity.User::getId).orElse(0L);
        }
    }

    private static UserService userSvc() { return AppContext.get(UserService.class); }
    private static TopicService topicSvc() { return AppContext.get(TopicService.class); }
    private static ArticleService articleSvc() { return AppContext.get(ArticleService.class); }
    private static MessageService msgSvc() { return AppContext.get(MessageService.class); }
    private static UserFollowService followSvc() { return AppContext.get(UserFollowService.class); }
    private static FavoriteService favSvc() { return AppContext.get(FavoriteService.class); }

}
