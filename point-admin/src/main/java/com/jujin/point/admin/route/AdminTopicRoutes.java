package com.jujin.point.admin.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.service.TopicService;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.util.Map;

public class AdminTopicRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/topic",
            // List all topics with pagination
            Route.get("", ctx -> {
                var pr = PageRequest.of(
                    ctx.queryParam("page", Integer.class).orElse(1),
                    ctx.queryParam("pageSize", Integer.class).orElse(20)
                );
                var result = svc().getRecentTopics(pr);
                ctx.sendJson(200, ApiResponse.ok(result));
            }),
            // Delete/undelete topic
            Route.post("/delete/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                var topic = svc().findById(id).orElse(null);
                if (topic == null) {
                    ctx.sendJson(404, ApiResponse.error("帖子不存在"));
                    return;
                }
                svc().delete(topic.getUserId(), id);
                com.jujin.point.admin.AdminAudit.log(ctx, "delete", "topic", id,
                    "删除帖子: " + (topic.getTitle() != null ? topic.getTitle() : "#" + id));
                ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "deleted", true)));
            }),
            // Recommend/unrecommend topic
            Route.post("/recommend/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                boolean recommend = "true".equals(ctx.queryParam("recommend").orElse(null));
                svc().recommend(id, recommend);
                com.jujin.point.admin.AdminAudit.log(ctx, "recommend", "topic", id,
                    (recommend ? "推荐" : "取消推荐") + "帖子 #" + id);
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Sticky/unsticky topic
            Route.post("/sticky/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                boolean sticky = "true".equals(ctx.queryParam("sticky").orElse(null));
                svc().sticky(id, sticky);
                com.jujin.point.admin.AdminAudit.log(ctx, "sticky", "topic", id,
                    (sticky ? "置顶" : "取消置顶") + "帖子 #" + id);
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Search topics
            Route.get("/search", ctx -> {
                String q = ctx.queryParam("q").orElse(null);
                var pr = PageRequest.of(
                    ctx.queryParam("page", Integer.class).orElse(1),
                    ctx.queryParam("pageSize", Integer.class).orElse(50)
                );
                var result = svc().search(q != null ? q : "", pr);
                ctx.sendJson(200, ApiResponse.ok(result));
            }),
            // Accept/unaccept answer for QA
            Route.post("/accept_answer/{id}", ctx -> {
                long topicId = ctx.pathVar("id", Long.class).orElse(0L);
                long commentId;
                try {
                    commentId = Long.parseLong(ctx.queryParam("commentId").orElse("0"));
                } catch (NumberFormatException e) {
                    ctx.sendJson(400, ApiResponse.error("commentId 参数无效"));
                    return;
                }
                svc().acceptAnswer(topicId, commentId);
                com.jujin.point.admin.AdminAudit.log(ctx, "accept_answer", "topic", topicId,
                    "采纳答案 #" + commentId);
                ctx.sendJson(200, ApiResponse.ok());
            })
        );
    }

    private static TopicService svc() { return AppContext.get(TopicService.class); }
}
