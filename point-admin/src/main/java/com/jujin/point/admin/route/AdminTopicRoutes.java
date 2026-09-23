package com.jujin.point.admin.route;

import com.jujin.point.admin.AdminAudit;
import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.service.TopicService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.util.Map;

public class AdminTopicRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/topic",
            // List all topics with pagination
            Route.get("", ListTopicsHandler.class),
            // Delete/undelete topic
            Route.post("/delete/{id}", DeleteTopicHandler.class),
            // Recommend/unrecommend topic
            Route.post("/recommend/{id}", RecommendTopicHandler.class),
            // Sticky/unsticky topic
            Route.post("/sticky/{id}", StickyTopicHandler.class),
            // Search topics
            Route.get("/search", SearchTopicsHandler.class),
            // Accept/unaccept answer for QA
            Route.post("/accept_answer/{id}", AcceptAnswerHandler.class)
        );
    }

    public static final class ListTopicsHandler implements RouteHandler {
        private final TopicService svc;

        public ListTopicsHandler(TopicService svc) {
            this.svc = svc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var pr = PageRequest.of(
                ctx.queryParam("page", Integer.class).orElse(1),
                ctx.queryParam("pageSize", Integer.class).orElse(20)
            );
            var result = svc.getRecentTopics(pr);
            ctx.sendJson(200, ApiResponse.ok(result));
        }
    }

    public static final class DeleteTopicHandler implements RouteHandler {
        private final TopicService svc;
        private final AdminAudit audit;
        private final AuthApi authApi;

        public DeleteTopicHandler(TopicService svc, AdminAudit audit, AuthApi authApi) {
            this.svc = svc;
            this.audit = audit;
            this.authApi = authApi;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var topic = svc.findById(id).orElse(null);
            if (topic == null) {
                ctx.sendJson(404, ApiResponse.error("帖子不存在"));
                return;
            }
            svc.deleteAsAdmin(authApi.currentUserId(), id);
            audit.log(ctx, "delete", "topic", id,
                "删除帖子: " + (topic.getTitle() != null ? topic.getTitle() : "#" + id));
            ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "deleted", true)));
        }
    }

    public static final class RecommendTopicHandler implements RouteHandler {
        private final TopicService svc;
        private final AdminAudit audit;

        public RecommendTopicHandler(TopicService svc, AdminAudit audit) {
            this.svc = svc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            boolean recommend = "true".equals(ctx.queryParam("recommend").orElse(null));
            svc.recommend(id, recommend);
            audit.log(ctx, "recommend", "topic", id,
                (recommend ? "推荐" : "取消推荐") + "帖子 #" + id);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class StickyTopicHandler implements RouteHandler {
        private final TopicService svc;
        private final AdminAudit audit;

        public StickyTopicHandler(TopicService svc, AdminAudit audit) {
            this.svc = svc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            boolean sticky = "true".equals(ctx.queryParam("sticky").orElse(null));
            svc.sticky(id, sticky);
            audit.log(ctx, "sticky", "topic", id,
                (sticky ? "置顶" : "取消置顶") + "帖子 #" + id);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class SearchTopicsHandler implements RouteHandler {
        private final TopicService svc;

        public SearchTopicsHandler(TopicService svc) {
            this.svc = svc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            String q = ctx.queryParam("q").orElse(null);
            var pr = PageRequest.of(
                ctx.queryParam("page", Integer.class).orElse(1),
                ctx.queryParam("pageSize", Integer.class).orElse(50)
            );
            var result = svc.search(q != null ? q : "", pr);
            ctx.sendJson(200, ApiResponse.ok(result));
        }
    }

    public static final class AcceptAnswerHandler implements RouteHandler {
        private final TopicService svc;
        private final AdminAudit audit;

        public AcceptAnswerHandler(TopicService svc, AdminAudit audit) {
            this.svc = svc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long topicId = ctx.pathVar("id", Long.class).orElse(0L);
            long commentId;
            try {
                commentId = Long.parseLong(ctx.queryParam("commentId").orElse("0"));
            } catch (NumberFormatException e) {
                ctx.sendJson(400, ApiResponse.error("commentId 参数无效"));
                return;
            }
            svc.acceptAnswer(topicId, commentId);
            audit.log(ctx, "accept_answer", "topic", topicId,
                "采纳答案 #" + commentId);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }
}
