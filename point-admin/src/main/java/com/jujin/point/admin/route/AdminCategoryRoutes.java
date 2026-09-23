package com.jujin.point.admin.route;

import com.jujin.point.admin.AdminAudit;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.entity.Category;
import com.jujin.point.service.CategoryService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.util.Map;

public class AdminCategoryRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/category",
            // List all categories
            Route.get("", ListCategoriesHandler.class),
            // Create category
            Route.post("", CreateCategoryHandler.class),
            // Update category
            Route.post("/{id}", UpdateCategoryHandler.class),
            // Delete category (soft-delete)
            Route.post("/delete/{id}", DeleteCategoryHandler.class)
        );
    }

    public static final class ListCategoriesHandler implements RouteHandler {
        private final CategoryService catSvc;

        public ListCategoriesHandler(CategoryService catSvc) {
            this.catSvc = catSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var tree = catSvc.getTree();
            ctx.sendJson(200, ApiResponse.ok(tree));
        }
    }

    public static final class CreateCategoryHandler implements RouteHandler {
        private final CategoryService catSvc;
        private final AdminAudit audit;

        public CreateCategoryHandler(CategoryService catSvc, AdminAudit audit) {
            this.catSvc = catSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = ctx.bodyAsJson(Map.class);
            if (req == null || req.get("name") == null) {
                ctx.sendJson(400, ApiResponse.error("缺少 name 参数"));
                return;
            }
            var cat = new Category();
            cat.setName((String) req.get("name"));
            cat.setParentId(req.get("parentId") != null ? ((Number) req.get("parentId")).longValue() : 0L);
            cat.setType((String) req.getOrDefault("type", "normal"));
            cat.setDescription((String) req.get("description"));
            cat.setSortNo(req.get("sortNo") != null ? ((Number) req.get("sortNo")).intValue() : 0);
            cat.setStatus(1);
            cat.setCreateTime(System.currentTimeMillis());
            catSvc.create(cat);
            audit.log(ctx, "create", "category", cat.getId(),
                "新建分类: " + cat.getName());
            ctx.sendJson(201, ApiResponse.ok(Map.of("created", true, "id", cat.getId())));
        }
    }

    public static final class UpdateCategoryHandler implements RouteHandler {
        private final CategoryService catSvc;
        private final AdminAudit audit;

        public UpdateCategoryHandler(CategoryService catSvc, AdminAudit audit) {
            this.catSvc = catSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var existing = catSvc.findById(id);
            if (existing.isEmpty()) { ctx.sendJson(404, ApiResponse.error("分类不存在")); return; }
            var cat = existing.get();
            var req = ctx.bodyAsJson(Map.class);
            if (req == null) {
                ctx.sendJson(400, ApiResponse.error("请求体不能为空"));
                return;
            }
            if (req.containsKey("name")) cat.setName((String) req.get("name"));
            if (req.containsKey("parentId")) cat.setParentId(((Number) req.get("parentId")).longValue());
            if (req.containsKey("type")) cat.setType((String) req.get("type"));
            if (req.containsKey("description")) cat.setDescription((String) req.get("description"));
            if (req.containsKey("sortNo")) cat.setSortNo(((Number) req.get("sortNo")).intValue());
            if (req.containsKey("status")) cat.setStatus(((Number) req.get("status")).intValue());
            catSvc.update(cat);
            audit.log(ctx, "update", "category", id,
                "更新分类: " + cat.getName());
            ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "updated", true)));
        }
    }

    public static final class DeleteCategoryHandler implements RouteHandler {
        private final CategoryService catSvc;
        private final AdminAudit audit;

        public DeleteCategoryHandler(CategoryService catSvc, AdminAudit audit) {
            this.catSvc = catSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var existing = catSvc.findById(id);
            if (existing.isEmpty()) { ctx.sendJson(404, ApiResponse.error("分类不存在")); return; }
            catSvc.delete(id);
            audit.log(ctx, "delete", "category", id,
                "删除分类: " + existing.get().getName());
            ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "deleted", true)));
        }
    }
}
