package com.jujin.point.web.route;

import static com.jujin.point.web.WebUtils.intParam;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.CommentDtos.*;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.TopicDtos.*;
import com.jujin.point.service.*;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import java.util.*;

public class TopicRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of(
            "/api/topics",
            // List recent topics
            Route.get("", ctx -> {
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var result = topicSvc().getRecentTopics(
                    PageRequest.of(page, pageSize)
                );
                var enriched = ResponseEnricher.enrichTopics(result.items());
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", enriched);
                resp.put("page", result.page());
                resp.put("pageSize", result.pageSize());
                resp.put("total", result.total());
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            // Following feed
            Route.get("/following", ctx -> {
                var user = AuthFilter.requireUser();
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var db = AppContext.get(Database.class);
                int offset = (page - 1) * pageSize;
                // Get followed user IDs
                var followedIds = db
                    .query(
                        "SELECT other_id FROM bbs_user_follow WHERE user_id = ? AND status = 1",
                        user.userId()
                    )
                    .list(Row.class)
                    .stream()
                    .map(r -> r.longVal("other_id"))
                    .toList();
                if (followedIds.isEmpty()) {
                    ctx.sendJson(200, ApiResponse.ok(java.util.List.of()));
                    return;
                }
                var placeholders = followedIds
                    .stream()
                    .map(id -> "?")
                    .reduce((a, b) -> a + "," + b)
                    .orElse("?");
                var params = new Object[followedIds.size() + 2];
                for (int i = 0; i < followedIds.size(); i++) params[i] =
                    followedIds.get(i);
                params[followedIds.size()] = pageSize;
                params[followedIds.size() + 1] = offset;
                var tweets = db
                    .query(
                        "SELECT * FROM bbs_topic WHERE user_id IN (" +
                            placeholders +
                            ") AND status = 1 AND type = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
                        params
                    )
                    .list(com.jujin.point.domain.entity.Topic.class);
                var enriched = ResponseEnricher.enrichTopics(tweets);
                ctx.sendJson(200, ApiResponse.ok(enriched));
            }),
            // Moments (tweets — type=1)
            Route.get("/moments", ctx -> {
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var db = AppContext.get(Database.class);
                int offset = (page - 1) * pageSize;
                var tweets = db
                    .query(
                        "SELECT * FROM bbs_topic WHERE type = 1 AND status = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
                        pageSize,
                        offset
                    )
                    .list(com.jujin.point.domain.entity.Topic.class);
                var enriched = ResponseEnricher.enrichTopics(tweets);
                ctx.sendJson(200, ApiResponse.ok(enriched));
            }),
            // Recommended topics
            Route.get("/recommended", ctx -> {
                int limit = intParam(ctx, "limit", 10);
                var topics = topicSvc().getRecommended(limit);
                ctx.sendJson(
                    200,
                    ApiResponse.ok(ResponseEnricher.enrichTopics(topics))
                );
            }),
            // Search
            Route.get("/search", ctx -> {
                String q = ctx.queryParam("q").orElse(null);
                if (q == null || q.isBlank()) {
                    var empty = new LinkedHashMap<String, Object>();
                    empty.put("items", List.of());
                    empty.put("page", 1);
                    empty.put("pageSize", 30);
                    empty.put("total", 0);
                    ctx.sendJson(200, ApiResponse.ok(empty));
                    return;
                }
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var r = topicSvc().search(q, PageRequest.of(page, pageSize));
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", ResponseEnricher.enrichTopics(r.items()));
                resp.put("page", r.page());
                resp.put("pageSize", r.pageSize());
                resp.put("total", r.total());
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            // Topic detail
            Route.get("/{id}", ctx -> {
                var t = topicSvc()
                    .findById(ctx.pathVar("id", Long.class).orElse(0L))
                    .orElse(null);
                if (t == null) {
                    ctx.sendJson(404, ApiResponse.error("帖子不存在"));
                    return;
                }
                topicSvc().incrViewCount(t.getId());
                ctx.sendJson(
                    200,
                    ApiResponse.ok(ResponseEnricher.enrichTopic(t))
                );
            }),
            // Create
            Route.post("", CreateTopicRequest.class, (ctx, req) -> {
                var user = AuthFilter.requireUser();
                var t = topicSvc().create(user.userId(), req);
                ctx.sendJson(
                    201,
                    ApiResponse.ok(ResponseEnricher.enrichTopic(t))
                );
            }),
            // Edit
            Route.post("/edit/{id}", UpdateTopicRequest.class, (ctx, req) -> {
                var user = AuthFilter.requireUser();
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                var t = topicSvc().update(user.userId(), id, req);
                ctx.sendJson(
                    200,
                    ApiResponse.ok(ResponseEnricher.enrichTopic(t))
                );
            }),
            // Delete
            Route.post("/delete/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                topicSvc().delete(
                    user.userId(),
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),

            // --- Comments (nested under topic) ---
            Route.get("/{id}/comments", ctx -> {
                long topicId = ctx.pathVar("id", Long.class).orElse(0L);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var comments = commentSvc().getComments(
                    "topic",
                    topicId,
                    PageRequest.of(page, pageSize)
                );
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", ResponseEnricher.enrichComments(comments));
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", commentSvc().countComments("topic", topicId));
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.post(
                "/{id}/comments",
                CreateCommentRequest.class,
                (ctx, req) -> {
                    var user = AuthFilter.requireUser();
                    long topicId = ctx.pathVar("id", Long.class).orElse(0L);
                    var c = commentSvc().create(
                        user.userId(),
                        "topic",
                        topicId,
                        req.content(),
                        req.contentType(),
                        req.imageList(),
                        req.quoteId() != null ? req.quoteId() : 0
                    );
                    ctx.sendJson(
                        201,
                        ApiResponse.ok(ResponseEnricher.enrichComment(c))
                    );
                }
            ),

            // --- Comment Delete ---
            Route.post("/{id}/comments/{commentId}/delete", ctx -> {
                var user = AuthFilter.requireUser();
                commentSvc().delete(
                    user.userId(),
                    ctx.pathVar("commentId", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),

            // --- Comment Like ---
            Route.post("/{id}/comments/{commentId}/like", ctx -> {
                var user = AuthFilter.requireUser();
                long commentId = ctx
                    .pathVar("commentId", Long.class)
                    .orElse(0L);
                boolean liked = likeSvc().like(
                    user.userId(),
                    "comment",
                    commentId
                );
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),
            Route.post("/{id}/comments/{commentId}/unlike", ctx -> {
                var user = AuthFilter.requireUser();
                likeSvc().unlike(
                    user.userId(),
                    "comment",
                    ctx.pathVar("commentId", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),

            // --- Like (nested) ---
            Route.post("/{id}/like", ctx -> {
                var user = AuthFilter.requireUser();
                long topicId = ctx.pathVar("id", Long.class).orElse(0L);
                boolean liked = likeSvc().like(user.userId(), "topic", topicId);
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),
            Route.post("/{id}/unlike", ctx -> {
                var user = AuthFilter.requireUser();
                likeSvc().unlike(
                    user.userId(),
                    "topic",
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/like/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean liked =
                    user != null &&
                    likeSvc().hasLiked(
                        user.userId(),
                        "topic",
                        ctx.pathVar("id", Long.class).orElse(0L)
                    );
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),

            // --- Favorite (nested) ---
            Route.post("/{id}/favorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().add(
                    user.userId(),
                    "topic",
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/unfavorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().remove(
                    user.userId(),
                    "topic",
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/favorite/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean f =
                    user != null &&
                    favSvc().isFavorited(
                        user.userId(),
                        "topic",
                        ctx.pathVar("id", Long.class).orElse(0L)
                    );
                ctx.sendJson(200, ApiResponse.ok(Map.of("favorited", f)));
            })
        );
    }

    private static TopicService topicSvc() {
        return AppContext.get(TopicService.class);
    }

    private static CommentService commentSvc() {
        return AppContext.get(CommentService.class);
    }

    private static UserLikeService likeSvc() {
        return AppContext.get(UserLikeService.class);
    }

    private static FavoriteService favSvc() {
        return AppContext.get(FavoriteService.class);
    }
}
