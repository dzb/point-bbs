package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.CommentDtos.*;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.TopicDtos.*;
import com.jujin.point.service.*;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

import java.util.*;

public class TopicRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/topics",
            // List recent topics
            Route.get("", ctx -> {
                int page = parseInt(ctx.queryParam("page"), 1);
                int pageSize = parseInt(ctx.queryParam("pageSize"), 20);
                var result = topicSvc().getRecentTopics(PageRequest.of(page, pageSize));
                var enriched = result.items().stream().map(ResponseEnricher::enrichTopic).toList();
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", enriched); resp.put("page", result.page());
                resp.put("pageSize", result.pageSize()); resp.put("total", result.total());
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            // Following feed
            Route.get("/following", ctx -> {
                var user = AuthFilter.requireUser();
                int page = parseInt(ctx.queryParam("page"), 1);
                int pageSize = parseInt(ctx.queryParam("pageSize"), 20);
                var db = AppContext.get(com.jujin.freeway.db.Database.class);
                int offset = (page - 1) * pageSize;
                // Get followed user IDs
                var followedIds = db.query(
                    "SELECT other_id FROM bbs_user_follow WHERE user_id = ? AND status = 1", user.userId()
                ).list(com.jujin.freeway.db.Row.class).stream()
                    .map(r -> r.integer("other_id").toString()).toList();
                if (followedIds.isEmpty()) {
                    ctx.sendJson(200, ApiResponse.ok(java.util.List.of()));
                    return;
                }
                var inClause = String.join(",", followedIds);
                var tweets = db.query(
                    "SELECT * FROM bbs_topic WHERE user_id IN (" + inClause + ") AND status = 1 AND type = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
                    pageSize, offset
                ).list(com.jujin.point.domain.entity.Topic.class);
                var enriched = tweets.stream().map(com.jujin.point.web.ResponseEnricher::enrichTopic).toList();
                ctx.sendJson(200, ApiResponse.ok(enriched));
            }),
            // Moments (tweets — type=1)
            Route.get("/moments", ctx -> {
                int page = TopicRoutes.parseInt(ctx.queryParam("page"), 1);
                int pageSize = TopicRoutes.parseInt(ctx.queryParam("pageSize"), 20);
                var db = AppContext.get(com.jujin.freeway.db.Database.class);
                int offset = (page - 1) * pageSize;
                var tweets = db.query(
                    "SELECT * FROM bbs_topic WHERE type = 1 AND status = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
                    pageSize, offset
                ).list(com.jujin.point.domain.entity.Topic.class);
                var enriched = tweets.stream().map(com.jujin.point.web.ResponseEnricher::enrichTopic).toList();
                ctx.sendJson(200, ApiResponse.ok(enriched));
            }),
            // Recommended topics
            Route.get("/recommended", ctx -> {
                var topics = topicSvc().getRecommended(parseInt(ctx.queryParam("limit"), 10));
                ctx.sendJson(200, ApiResponse.ok(topics.stream().map(ResponseEnricher::enrichTopic).toList()));
            }),
            // Search
            Route.get("/search", ctx -> {
                String q = ctx.queryParam("q");
                if (q == null || q.isBlank()) { ctx.sendJson(200, ApiResponse.ok(List.of())); return; }
                var r = topicSvc().search(q, PageRequest.of(parseInt(ctx.queryParam("page"), 1), 20));
                ctx.sendJson(200, ApiResponse.ok(r.items().stream().map(ResponseEnricher::enrichTopic).toList()));
            }),
            // Topic detail
            Route.get("/{id}", ctx -> {
                var t = topicSvc().findById(ctx.pathVar("id", Long.class)).orElse(null);
                if (t == null) { ctx.sendJson(404, ApiResponse.error("帖子不存在")); return; }
                topicSvc().incrViewCount(t.getId());
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichTopic(t)));
            }),
            // Create
            Route.post("", ctx -> {
                var user = AuthFilter.requireUser();
                var t = topicSvc().create(user.userId(), ctx.bodyAsJson(CreateTopicRequest.class));
                ctx.sendJson(201, ApiResponse.ok(ResponseEnricher.enrichTopic(t)));
            }),
            // Edit
            Route.post("/edit/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                long id = ctx.pathVar("id", Long.class);
                var t = topicSvc().update(user.userId(), id, ctx.bodyAsJson(UpdateTopicRequest.class));
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichTopic(t)));
            }),
            // Delete
            Route.post("/delete/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                topicSvc().delete(user.userId(), ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),

            // --- Comments (nested under topic) ---
            Route.get("/{id}/comments", ctx -> {
                long topicId = ctx.pathVar("id", Long.class);
                int page = parseInt(ctx.queryParam("page"), 1);
                var comments = commentSvc().getComments("topic", topicId, PageRequest.of(page, 20));
                ctx.sendJson(200, ApiResponse.ok(comments.stream().map(ResponseEnricher::enrichComment).toList()));
            }),
            Route.post("/{id}/comments", ctx -> {
                var user = AuthFilter.requireUser();
                long topicId = ctx.pathVar("id", Long.class);
                var req = ctx.bodyAsJson(CreateCommentRequest.class);
                var c = commentSvc().create(user.userId(), "topic", topicId, req.content(), req.contentType(),
                    req.imageList(), req.quoteId() != null ? req.quoteId() : 0);
                ctx.sendJson(201, ApiResponse.ok(ResponseEnricher.enrichComment(c)));
            }),

            // --- Like (nested) ---
            Route.post("/{id}/like", ctx -> {
                var user = AuthFilter.requireUser();
                long topicId = ctx.pathVar("id", Long.class);
                boolean liked = likeSvc().like(user.userId(), "topic", topicId);
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),
            Route.post("/{id}/unlike", ctx -> {
                var user = AuthFilter.requireUser();
                likeSvc().unlike(user.userId(), "topic", ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/like/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean liked = user != null && likeSvc().hasLiked(user.userId(), "topic", ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),

            // --- Favorite (nested) ---
            Route.post("/{id}/favorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().add(user.userId(), "topic", ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/unfavorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().remove(user.userId(), "topic", ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/favorite/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean f = user != null && favSvc().isFavorited(user.userId(), "topic", ctx.pathVar("id", Long.class));
                ctx.sendJson(200, ApiResponse.ok(Map.of("favorited", f)));
            })
        );
    }

    private static TopicService topicSvc() { return AppContext.get(TopicService.class); }
    private static CommentService commentSvc() { return AppContext.get(CommentService.class); }
    private static UserLikeService likeSvc() { return AppContext.get(UserLikeService.class); }
    private static FavoriteService favSvc() { return AppContext.get(FavoriteService.class); }

    static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
