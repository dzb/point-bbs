package com.jujin.point.web.route;

import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.ArticleDtos.*;
import com.jujin.point.service.ArticleService;
import com.jujin.point.service.CommentService;
import com.jujin.point.service.FavoriteService;
import com.jujin.point.service.UserLikeService;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.util.*;

import static com.jujin.point.web.WebUtils.intParam;

public class ArticleRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/articles",
            Route.get("", ListArticlesHandler.class),
            Route.get("/{id}", ArticleDetailHandler.class),
            Route.post("", CreateArticleHandler.class),
            Route.post("/edit/{id}", EditArticleHandler.class),
            Route.post("/delete/{id}", DeleteArticleHandler.class),
            // Like
            Route.post("/{id}/like", LikeArticleHandler.class),
            Route.post("/{id}/unlike", UnlikeArticleHandler.class),
            // Favorite
            Route.post("/{id}/favorite", FavoriteArticleHandler.class),
            Route.post("/{id}/unfavorite", UnfavoriteArticleHandler.class),
            Route.get("/{id}/like/status", ArticleLikeStatusHandler.class),
            Route.get("/{id}/favorite/status", ArticleFavoriteStatusHandler.class),
            // Comments
            Route.get("/{id}/comments", ArticleCommentsHandler.class),
            Route.post("/{id}/comments", CreateArticleCommentHandler.class)
        );
    }

    public static final class ListArticlesHandler implements RouteHandler {
        private final ArticleService svc;
        private final ResponseEnricher enricher;

        public ListArticlesHandler(ArticleService svc, ResponseEnricher enricher) {
            this.svc = svc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var pr = com.jujin.point.domain.dto.PageRequest.of(
                intParam(ctx, "page", 1),
                intParam(ctx, "pageSize", 30)
            );
            var articles = svc.getRecent(pr.page(), pr.pageSize());
            ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(
                enricher.enrichArticles(articles),
                pr.page(), pr.pageSize(), svc.countRecent())));
        }
    }

    public static final class ArticleDetailHandler implements RouteHandler {
        private final ArticleService svc;
        private final ResponseEnricher enricher;

        public ArticleDetailHandler(ArticleService svc, ResponseEnricher enricher) {
            this.svc = svc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var a = svc.findById(ctx.pathVar("id", Long.class).orElse(0L)).orElse(null);
            if (a == null) { ctx.sendJson(404, ApiResponse.error("文章不存在")); return; }
            ctx.sendJson(200, ApiResponse.ok(enricher.enrichArticle(a)));
        }
    }

    public static final class CreateArticleHandler implements RouteHandler {
        private final ArticleService svc;
        private final ResponseEnricher enricher;

        public CreateArticleHandler(ArticleService svc, ResponseEnricher enricher) {
            this.svc = svc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, CreateArticleRequest.class);
            var user = AuthFilter.requireUser();
            var a = svc.create(user.userId(), req.title(), req.summary(), req.content(), req.contentType());
            if (req.cover() != null) svc.updateCover(a.getId(), req.cover());
            if (req.tags() != null) svc.updateTags(a.getId(), req.tags());
            if (req.sourceUrl() != null) svc.updateSourceUrl(a.getId(), req.sourceUrl());
            ctx.sendJson(201, ApiResponse.ok(enricher.enrichArticle(svc.findById(a.getId()).orElse(a))));
        }
    }

    public static final class EditArticleHandler implements RouteHandler {
        private final ArticleService svc;
        private final ResponseEnricher enricher;

        public EditArticleHandler(ArticleService svc, ResponseEnricher enricher) {
            this.svc = svc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, UpdateArticleRequest.class);
            var user = AuthFilter.requireUser();
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var a = svc.findById(id).orElse(null);
            if (a == null || a.getUserId() != user.userId()) { ctx.sendJson(403, ApiResponse.error("无权操作")); return; }
            svc.update(id, req);
            ctx.sendJson(200, ApiResponse.ok(enricher.enrichArticle(svc.findById(id).orElse(a))));
        }
    }

    public static final class DeleteArticleHandler implements RouteHandler {
        private final ArticleService svc;

        public DeleteArticleHandler(ArticleService svc) {
            this.svc = svc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            svc.delete(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class LikeArticleHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public LikeArticleHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            boolean liked = likeSvc.like(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
        }
    }

    public static final class UnlikeArticleHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public UnlikeArticleHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            likeSvc.unlike(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class FavoriteArticleHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public FavoriteArticleHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            favSvc.add(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class UnfavoriteArticleHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public UnfavoriteArticleHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.requireUser();
            favSvc.remove(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class ArticleLikeStatusHandler implements RouteHandler {
        private final UserLikeService likeSvc;

        public ArticleLikeStatusHandler(UserLikeService likeSvc) {
            this.likeSvc = likeSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            boolean liked = user != null && likeSvc.hasLiked(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
        }
    }

    public static final class ArticleFavoriteStatusHandler implements RouteHandler {
        private final FavoriteService favSvc;

        public ArticleFavoriteStatusHandler(FavoriteService favSvc) {
            this.favSvc = favSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var user = AuthFilter.currentUser();
            boolean f = user != null && favSvc.isFavorited(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
            ctx.sendJson(200, ApiResponse.ok(Map.of("favorited", f)));
        }
    }

    public static final class ArticleCommentsHandler implements RouteHandler {
        private final CommentService commentSvc;
        private final ResponseEnricher enricher;

        public ArticleCommentsHandler(CommentService commentSvc, ResponseEnricher enricher) {
            this.commentSvc = commentSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long articleId = ctx.pathVar("id", Long.class).orElse(0L);
            int page = intParam(ctx, "page", 1);
            int pageSize = intParam(ctx, "pageSize", 30);
            var comments = commentSvc
                .getComments("article", articleId, com.jujin.point.domain.dto.PageRequest.of(page, pageSize));
            long total = commentSvc.countComments("article", articleId);
            ctx.sendJson(200, ApiResponse.ok(ApiResponse.page(
                enricher.enrichComments(comments), page, pageSize, total)));
        }
    }

    public static final class CreateArticleCommentHandler implements RouteHandler {
        private final CommentService commentSvc;
        private final ResponseEnricher enricher;

        public CreateArticleCommentHandler(CommentService commentSvc, ResponseEnricher enricher) {
            this.commentSvc = commentSvc;
            this.enricher = enricher;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = WebUtils.validatedBody(ctx, com.jujin.point.domain.dto.CommentDtos.CreateCommentRequest.class);
            var user = AuthFilter.requireUser();
            long articleId = ctx.pathVar("id", Long.class).orElse(0L);
            var c = commentSvc
                .create(user.userId(), "article", articleId, req.content(), req.contentType(), req.imageList(), req.quoteId() != null ? req.quoteId() : 0);
            ctx.sendJson(201, ApiResponse.ok(enricher.enrichComment(c)));
        }
    }
}
