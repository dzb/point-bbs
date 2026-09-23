package com.jujin.point.web.route;

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
import com.jujin.freeway.http.route.RouteHandler;

import java.util.*;

import static com.jujin.point.web.WebUtils.intParam;

public class UserRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/users",
            Route.get("/current", CurrentUserHandler.class),
            // Permission codes of the current user (drives admin UI visibility)
            Route.get("/current/permissions", ctx -> {
                var user = AuthFilter.currentUser();
                if (user == null) { ctx.sendJson(200, ApiResponse.ok(List.of())); return; }
                ctx.sendJson(200, ApiResponse.ok(user.roles()));
            }),
            // User search for @mention autocomplete
            Route.get("/search", SearchUsersHandler.class),
            Route.get("/{id}", UserProfileHandler.class),
            Route.post("/edit/{id}", EditUserProfileHandler.class),
            Route.get("/{id}/topics", UserTopicsHandler.class),
            Route.get("/{id}/articles", UserArticlesHandler.class),
            Route.get("/{id}/messages", UserMessagesHandler.class),
            Route.get("/{id}/messages/unread", UnreadMessagesHandler.class),
            Route.post("/{id}/messages/read", MarkMessagesReadHandler.class),
            Route.post("/{id}/messages/read-all", MarkAllMessagesReadHandler.class),
            Route.post("/{id}/follow", FollowUserHandler.class),
            Route.post("/{id}/unfollow", UnfollowUserHandler.class),
            Route.get("/{id}/follow/status", FollowStatusHandler.class),
            Route.get("/{id}/followers", FollowersHandler.class),
            Route.get("/{id}/following", FollowingHandler.class),
            Route.get("/{id}/favorites", UserFavoritesHandler.class)
        );
    }

    public static final class CurrentUserHandler implements RouteHandler {
        private final UserService userSvc;

        public CurrentUserHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            if (user == null) { ctx.sendJson(200, ApiResponse.ok(null)); return; }
            var full = userSvc.findById(user.userId());
            full.ifPresent(u -> u.setPassword(null));
            ctx.sendJson(200, ApiResponse.ok(full.orElse(null)));
        }
    }

    public static final class SearchUsersHandler implements RouteHandler {
        private final UserService userSvc;

        public SearchUsersHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            String q = ctx.queryParam("q").orElse(null);
            if (q == null || q.isBlank()) {
                ctx.sendJson(200, ApiResponse.ok(List.of()));
                return;
            }
            var users = userSvc.searchByPrefix(q.trim(), 10);
            users.forEach(u -> u.setPassword(null));
            ctx.sendJson(200, ApiResponse.ok(users));
        }
    }

    public static final class UserProfileHandler implements RouteHandler {
        private final UserService userSvc;

        public UserProfileHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var u = userSvc.findById(resolveUserId(userSvc, ctx));
            // Public profile — never expose credentials or contact info
            u.ifPresent(UserRoutes::sanitizePublic);
            ctx.sendJson(200, ApiResponse.ok(u.orElse(null)));
        }
    }

    public static final class EditUserProfileHandler implements RouteHandler {
        private final UserService userSvc;

        public EditUserProfileHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long targetId = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != targetId) {
                ctx.sendJson(403, ApiResponse.error(403, "只能编辑自己的资料"));
                return;
            }
            var editReq = ctx.bodyAsJson(UpdateUserRequest.class);
            if (editReq == null) {
                ctx.sendJson(400, ApiResponse.error("请求体不能为空"));
                return;
            }
            userSvc.updateUser(targetId, editReq);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class UserTopicsHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final UserService userSvc;
        private final ResponseEnricher enricher;

        public UserTopicsHandler(TopicService topicSvc, UserService userSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.userSvc = userSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long uid = resolveUserId(userSvc, ctx);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var r = topicSvc.getUserTopics(uid, PageRequest.of(page, pageSize));
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(enricher.enrichTopics(r.items()),
                    r.page(), r.pageSize(), r.total())));
        }
    }

    public static final class UserArticlesHandler implements RouteHandler {
        private final ArticleService articleSvc;
        private final UserService userSvc;
        private final ResponseEnricher enricher;

        public UserArticlesHandler(ArticleService articleSvc, UserService userSvc, ResponseEnricher enricher) {
            this.articleSvc = articleSvc;
            this.userSvc = userSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long uid = resolveUserId(userSvc, ctx);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var items = articleSvc.getByUser(uid, page, pageSize);
            var total = articleSvc.countByUser(uid);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(enricher.enrichArticles(items), page, pageSize, total)));
        }
    }

    public static final class UserMessagesHandler implements RouteHandler {
        private final MessageService msgSvc;

        public UserMessagesHandler(MessageService msgSvc) {
            this.msgSvc = msgSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long uid = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var items = msgSvc.getUserMessages(uid, page, pageSize);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(items, page, pageSize, msgSvc.count(uid))));
        }
    }

    public static final class UnreadMessagesHandler implements RouteHandler {
        private final MessageService msgSvc;

        public UnreadMessagesHandler(MessageService msgSvc) {
            this.msgSvc = msgSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long uid = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的消息")); return; }
            ctx.sendJson(200, ApiResponse.ok(Map.of("count", msgSvc.unreadCount(uid))));
        }
    }

    public static final class MarkMessagesReadHandler implements RouteHandler {
        private final MessageService msgSvc;

        public MarkMessagesReadHandler(MessageService msgSvc) {
            this.msgSvc = msgSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long uid = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能操作自己的消息")); return; }
            var req = ctx.bodyAsJson(Map.class);
            if (req == null || req.get("ids") == null) {
                ctx.sendJson(400, ApiResponse.error("缺少 ids 参数"));
                return;
            }
            @SuppressWarnings("unchecked")
            var ids = ((List<Number>) req.get("ids")).stream().map(Number::longValue).toList();
            msgSvc.markRead(uid, ids);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class MarkAllMessagesReadHandler implements RouteHandler {
        private final MessageService msgSvc;

        public MarkAllMessagesReadHandler(MessageService msgSvc) {
            this.msgSvc = msgSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long uid = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能操作自己的消息")); return; }
            msgSvc.markAllRead(uid);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class FollowUserHandler implements RouteHandler {
        private final UserFollowService followSvc;

        public FollowUserHandler(UserFollowService followSvc) {
            this.followSvc = followSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            followSvc.follow(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class UnfollowUserHandler implements RouteHandler {
        private final UserFollowService followSvc;

        public UnfollowUserHandler(UserFollowService followSvc) {
            this.followSvc = followSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            followSvc.unfollow(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class FollowStatusHandler implements RouteHandler {
        private final UserFollowService followSvc;
        private final UserService userSvc;

        public FollowStatusHandler(UserFollowService followSvc, UserService userSvc) {
            this.followSvc = followSvc;
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            boolean f = user != null && followSvc.isFollowing(user.userId(), resolveUserId(userSvc, ctx));
            ctx.sendJson(200, ApiResponse.ok(Map.of("following", f)));
        }
    }

    public static final class FollowersHandler implements RouteHandler {
        private final UserFollowService followSvc;
        private final UserService userSvc;

        public FollowersHandler(UserFollowService followSvc, UserService userSvc) {
            this.followSvc = followSvc;
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long uid = resolveUserId(userSvc, ctx);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var items = followSvc.getFollowers(uid, page, pageSize);
            items.forEach(UserRoutes::sanitizePublic);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(items, page, pageSize, followSvc.countFollowers(uid))));
        }
    }

    public static final class FollowingHandler implements RouteHandler {
        private final UserFollowService followSvc;
        private final UserService userSvc;

        public FollowingHandler(UserFollowService followSvc, UserService userSvc) {
            this.followSvc = followSvc;
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long uid = resolveUserId(userSvc, ctx);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var items = followSvc.getFollowing(uid, page, pageSize);
            items.forEach(UserRoutes::sanitizePublic);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(items, page, pageSize, followSvc.countFollowing(uid))));
        }
    }

    public static final class UserFavoritesHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public UserFavoritesHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long uid = ctx.pathVar("id", Long.class).orElse(0L);
            if (user.userId() != uid) { ctx.sendJson(403, ApiResponse.error(403, "只能查看自己的收藏")); return; }
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var items = favSvc.getUserFavorites(uid, page, pageSize);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(items, page, pageSize, favSvc.count(uid))));
        }
    }

    /** Resolve a path variable to a user ID, accepting both numeric IDs and usernames. */
    private static long resolveUserId(UserService userSvc, HttpContext ctx) {
        var idStr = ctx.pathVar("id", String.class).orElse(null);
        try { return Long.parseLong(idStr); }
        catch (NumberFormatException e) {
            return userSvc.findByUsername(idStr)
                .map(com.jujin.point.domain.entity.User::getId).orElse(0L);
        }
    }

    /** Strip credentials and private contact info before exposing a user in public responses. */
    private static void sanitizePublic(com.jujin.point.domain.entity.User u) {
        u.setPassword(null);
        u.setEmail(null);
        u.setPhone(null);
        u.setBirthday(null);
        u.setHomePage(null);
    }
}
