package com.jujin.point.web.route;

import static com.jujin.point.web.WebUtils.intParam;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;
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
            Route.get("", RecentTopicsHandler.class),
            Route.get("/following", FollowingFeedHandler.class),
            Route.get("/moments", MomentsHandler.class),
            Route.get("/recommended", RecommendedTopicsHandler.class),
            Route.get("/search", SearchTopicsHandler.class),
            Route.get("/{id}", TopicDetailHandler.class),
            Route.post("", CreateTopicHandler.class),
            Route.post("/edit/{id}", EditTopicHandler.class),
            Route.post("/delete/{id}", DeleteTopicHandler.class),

            // --- Comments (nested under topic) ---
            Route.get("/{id}/comments", TopicCommentsHandler.class),
            Route.post("/{id}/comments", CreateTopicCommentHandler.class),

            // --- Comment Delete ---
            Route.post("/{id}/comments/{commentId}/delete", DeleteTopicCommentHandler.class),

            // --- Comment Like ---
            Route.post("/{id}/comments/{commentId}/like", LikeCommentHandler.class),
            Route.post("/{id}/comments/{commentId}/unlike", UnlikeCommentHandler.class),

            // --- Like (nested) ---
            Route.post("/{id}/like", LikeTopicHandler.class),
            Route.post("/{id}/unlike", UnlikeTopicHandler.class),
            Route.get("/{id}/like/status", TopicLikeStatusHandler.class),

            // --- Favorite (nested) ---
            Route.post("/{id}/favorite", FavoriteTopicHandler.class),
            Route.post("/{id}/unfavorite", UnfavoriteTopicHandler.class),
            Route.get("/{id}/favorite/status", TopicFavoriteStatusHandler.class)
        );
    }

    // ──── list / feed ────

    public static final class RecentTopicsHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public RecentTopicsHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var result = topicSvc.getRecentTopics(
                PageRequest.of(page, pageSize)
            );
            var enriched = enricher.enrichTopics(result.items());
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(enriched, result.page(), result.pageSize(), result.total())));
        }
    }

    public static final class FollowingFeedHandler implements RouteHandler {
        private final Database db;
        private final ResponseEnricher enricher;

        public FollowingFeedHandler(Database db, ResponseEnricher enricher) {
            this.db = db;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var pr = PageRequest.of(page, pageSize);
            int offset = (int) pr.offset();
            // Get followed user IDs
            var followedIds = db
                .query(
                    "SELECT other_id FROM bbs_user_follow WHERE user_id = ? AND status = 1",
                    user.userId()
                )
                .list(Row.class)
                .stream()
                .map(r -> r.longValue("other_id"))
                .toList();
            if (followedIds.isEmpty()) {
                ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(List.of(), page, pageSize, 0)));
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
            var countParams = new Object[followedIds.size()];
            for (int i = 0; i < followedIds.size(); i++) countParams[i] = followedIds.get(i);
            var total = db
                .query(
                    "SELECT COUNT(*) AS cnt FROM bbs_topic WHERE user_id IN (" +
                        placeholders +
                        ") AND status = 1 AND type = 1",
                    countParams
                )
                .one(Row.class)
                .map(r -> r.longValue("cnt"))
                .orElse(0L);
            var enriched = enricher.enrichTopics(tweets);
            ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(enriched, page, pageSize, total)));
        }
    }

    public static final class MomentsHandler implements RouteHandler {
        private final Database db;
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public MomentsHandler(Database db, TopicService topicSvc, ResponseEnricher enricher) {
            this.db = db;
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var pr = PageRequest.of(page, pageSize);
            int offset = (int) pr.offset();
            var tweets = db
                .query(
                    "SELECT * FROM bbs_topic WHERE type = 1 AND status = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
                    pageSize,
                    offset
                )
                .list(com.jujin.point.domain.entity.Topic.class);
            var enriched = enricher.enrichTopics(tweets);
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(enriched, page, pageSize, topicSvc.countByType(1))));
        }
    }

    public static final class RecommendedTopicsHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public RecommendedTopicsHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            int limit = Math.min(Math.max(intParam(ctx, "limit", 10), 1), 50);
            var topics = topicSvc.getRecommended(limit);
            ctx.sendJson(
                200,
                ApiResponse.ok(Map.of("items", enricher.enrichTopics(topics)))
            );
        }
    }

    public static final class SearchTopicsHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public SearchTopicsHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            String q = ctx.queryParam("q").orElse(null);
            if (q == null || q.isBlank()) {
                ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(List.of(), 1, 30, 0)));
                return;
            }
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var r = topicSvc.search(q, PageRequest.of(page, pageSize));
            ctx.sendJson(200, ApiResponse.ok(
                ApiResponse.page(enricher.enrichTopics(r.items()),
                    r.page(), r.pageSize(), r.total())));
        }
    }

    // ──── detail / CRUD ────

    public static final class TopicDetailHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public TopicDetailHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var t = topicSvc
                .findById(ctx.pathVar("id", Long.class).orElse(0L))
                .orElse(null);
            if (t == null || t.getStatus() != 1) {
                ctx.sendJson(404, ApiResponse.error("帖子不存在"));
                return;
            }
            topicSvc.incrViewCount(t.getId());
            ctx.sendJson(
                200,
                ApiResponse.ok(enricher.enrichTopic(t))
            );
        }
    }

    public static final class CreateTopicHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public CreateTopicHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, CreateTopicRequest.class);
            var user = AuthFilter.requireUser();
            var t = topicSvc.create(user.userId(), req);
            ctx.sendJson(
                201,
                ApiResponse.ok(enricher.enrichTopic(t))
            );
        }
    }

    public static final class EditTopicHandler implements RouteHandler {
        private final TopicService topicSvc;
        private final ResponseEnricher enricher;

        public EditTopicHandler(TopicService topicSvc, ResponseEnricher enricher) {
            this.topicSvc = topicSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, UpdateTopicRequest.class);
            var user = AuthFilter.requireUser();
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var t = topicSvc.update(user.userId(), id, req);
            ctx.sendJson(
                200,
                ApiResponse.ok(enricher.enrichTopic(t))
            );
        }
    }

    public static final class DeleteTopicHandler implements RouteHandler {
        private final TopicService topicSvc;

        public DeleteTopicHandler(TopicService topicSvc) {
            this.topicSvc = topicSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            topicSvc.delete(
                user.userId(),
                ctx.pathVar("id", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    // ──── comments ────

    public static final class TopicCommentsHandler implements RouteHandler {
        private final CommentService commentSvc;
        private final ResponseEnricher enricher;

        public TopicCommentsHandler(CommentService commentSvc, ResponseEnricher enricher) {
            this.commentSvc = commentSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long topicId = ctx.pathVar("id", Long.class).orElse(0L);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var comments = commentSvc.getComments(
                "topic",
                topicId,
                PageRequest.of(page, pageSize)
            );
            ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(
                enricher.enrichComments(comments),
                page, pageSize, commentSvc.countComments("topic", topicId))));
        }
    }

    public static final class CreateTopicCommentHandler implements RouteHandler {
        private final CommentService commentSvc;
        private final ResponseEnricher enricher;

        public CreateTopicCommentHandler(CommentService commentSvc, ResponseEnricher enricher) {
            this.commentSvc = commentSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, CreateCommentRequest.class);
            var user = AuthFilter.requireUser();
            long topicId = ctx.pathVar("id", Long.class).orElse(0L);
            var c = commentSvc.create(
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
                ApiResponse.ok(enricher.enrichComment(c))
            );
        }
    }

    public static final class DeleteTopicCommentHandler implements RouteHandler {
        private final CommentService commentSvc;

        public DeleteTopicCommentHandler(CommentService commentSvc) {
            this.commentSvc = commentSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            commentSvc.delete(
                user.userId(),
                ctx.pathVar("commentId", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class LikeCommentHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public LikeCommentHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long commentId = ctx
                .pathVar("commentId", Long.class)
                .orElse(0L);
            boolean liked = likeSvc.like(
                user.userId(),
                "comment",
                commentId
            );
            ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
        }
    }

    public static final class UnlikeCommentHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public UnlikeCommentHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            likeSvc.unlike(
                user.userId(),
                "comment",
                ctx.pathVar("commentId", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    // ──── like ────

    public static final class LikeTopicHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public LikeTopicHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            long topicId = ctx.pathVar("id", Long.class).orElse(0L);
            boolean liked = likeSvc.like(user.userId(), "topic", topicId);
            ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
        }
    }

    public static final class UnlikeTopicHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public UnlikeTopicHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            likeSvc.unlike(
                user.userId(),
                "topic",
                ctx.pathVar("id", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class TopicLikeStatusHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public TopicLikeStatusHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            boolean liked =
                user != null &&
                likeSvc.hasLiked(
                    user.userId(),
                    "topic",
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
            ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
        }
    }

    // ──── favorite ────

    public static final class FavoriteTopicHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public FavoriteTopicHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            favSvc.add(
                user.userId(),
                "topic",
                ctx.pathVar("id", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class UnfavoriteTopicHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public UnfavoriteTopicHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            favSvc.remove(
                user.userId(),
                "topic",
                ctx.pathVar("id", Long.class).orElse(0L)
            );
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class TopicFavoriteStatusHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public TopicFavoriteStatusHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            boolean f =
                user != null &&
                favSvc.isFavorited(
                    user.userId(),
                    "topic",
                    ctx.pathVar("id", Long.class).orElse(0L)
                );
            ctx.sendJson(200, ApiResponse.ok(Map.of("favorited", f)));
        }
    }
}
