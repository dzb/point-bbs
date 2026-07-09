package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.ArticleDtos.*;
import com.jujin.point.service.ArticleService;
import com.jujin.point.service.FavoriteService;
import com.jujin.point.service.UserLikeService;
import com.jujin.point.web.ResponseEnricher;
import com.jujin.point.web.WebUtils;
import com.jujin.point.web.filter.AuthFilter;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.util.*;

import static com.jujin.point.web.WebUtils.intParam;

public class ArticleRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/articles",
            Route.get("", ctx -> {
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var articles = svc().getRecent(page, pageSize);
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichArticles(articles)));
            }),
            Route.get("/{id}", ctx -> {
                var a = svc().findById(ctx.pathVar("id", Long.class).orElse(0L)).orElse(null);
                if (a == null) { ctx.sendJson(404, ApiResponse.error("文章不存在")); return; }
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichArticle(a)));
            }),
            Route.post("", CreateArticleRequest.class, (ctx, req) -> {
                var user = AuthFilter.requireUser();
                var a = svc().create(user.userId(), req.title(), req.summary(), req.content(), req.contentType());
                if (req.cover() != null) svc().updateCover(a.getId(), req.cover());
                if (req.tags() != null) svc().updateTags(a.getId(), req.tags());
                if (req.sourceUrl() != null) svc().updateSourceUrl(a.getId(), req.sourceUrl());
                ctx.sendJson(201, ApiResponse.ok(ResponseEnricher.enrichArticle(svc().findById(a.getId()).orElse(a))));
            }),
            Route.post("/edit/{id}", UpdateArticleRequest.class, (ctx, req) -> {
                var user = AuthFilter.requireUser();
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                var a = svc().findById(id).orElse(null);
                if (a == null || a.getUserId() != user.userId()) { ctx.sendJson(403, ApiResponse.error("无权操作")); return; }
                svc().update(id, req.title(), req.summary(), req.content(), req.contentType(), req.cover(), req.sourceUrl());
                ctx.sendJson(200, ApiResponse.ok(ResponseEnricher.enrichArticle(svc().findById(id).orElse(a))));
            }),
            Route.post("/delete/{id}", ctx -> {
                var user = AuthFilter.requireUser();
                svc().delete(user.userId(), ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Like
            Route.post("/{id}/like", ctx -> {
                var user = AuthFilter.requireUser();
                likeSvc().like(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", true)));
            }),
            Route.post("/{id}/unlike", ctx -> {
                var user = AuthFilter.requireUser();
                likeSvc().unlike(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Favorite
            Route.post("/{id}/favorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().add(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.post("/{id}/unfavorite", ctx -> {
                var user = AuthFilter.requireUser();
                favSvc().remove(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok());
            }),
            Route.get("/{id}/like/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean liked = user != null && likeSvc().hasLiked(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok(Map.of("liked", liked)));
            }),
            Route.get("/{id}/favorite/status", ctx -> {
                var user = AuthFilter.currentUser();
                boolean f = user != null && favSvc().isFavorited(user.userId(), "article", ctx.pathVar("id", Long.class).orElse(0L));
                ctx.sendJson(200, ApiResponse.ok(Map.of("favorited", f)));
            }),
            // Comments
            Route.get("/{id}/comments", ctx -> {
                long articleId = ctx.pathVar("id", Long.class).orElse(0L);
                int page = intParam(ctx, "page", 1);
                int pageSize = intParam(ctx, "pageSize", 30);
                var comments = AppContext.get(com.jujin.point.service.CommentService.class)
                    .getComments("article", articleId, com.jujin.point.domain.dto.PageRequest.of(page, pageSize));
                var resp = new LinkedHashMap<String, Object>();
                resp.put("items", ResponseEnricher.enrichComments(comments));
                resp.put("page", page);
                resp.put("pageSize", pageSize);
                resp.put("total", AppContext.get(com.jujin.point.service.CommentService.class)
                    .countComments("article", articleId));
                ctx.sendJson(200, ApiResponse.ok(resp));
            }),
            Route.post("/{id}/comments", ctx -> {
                var user = AuthFilter.requireUser();
                long articleId = ctx.pathVar("id", Long.class).orElse(0L);
                var req = ctx.bodyAsJson(com.jujin.point.domain.dto.CommentDtos.CreateCommentRequest.class);
                var c = AppContext.get(com.jujin.point.service.CommentService.class)
                    .create(user.userId(), "article", articleId, req.content(), req.contentType(), req.imageList(), req.quoteId() != null ? req.quoteId() : 0);
                ctx.sendJson(201, ApiResponse.ok(com.jujin.point.web.ResponseEnricher.enrichComment(c)));
            })
        );
    }

    private static ArticleService svc() { return AppContext.get(ArticleService.class); }
    private static UserLikeService likeSvc() { return AppContext.get(UserLikeService.class); }
    private static FavoriteService favSvc() { return AppContext.get(FavoriteService.class); }

}
