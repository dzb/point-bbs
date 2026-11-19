package com.jujin.point.admin.route;

import com.jujin.point.admin.AdminAudit;
import com.jujin.point.admin.AdminBodies;
import com.jujin.point.domain.dto.AdminDtos.CreateCategoryRequest;
import com.jujin.point.domain.dto.AdminDtos.UpdateCategoryRequest;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.entity.Category;
import com.jujin.point.service.CategoryService;
import com.jujin.freeway.http.HttpExchange;
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
        public void handle(HttpExchange ctx) throws Exception {
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
        public void handle(HttpExchange ctx) throws Exception {
            var req = AdminBodies.validated(ctx, CreateCategoryRequest.class);
            var cat = new Category();
            cat.setName(req.name());
            cat.setParentId(req.parentId() != null ? req.parentId() : 0L);
            cat.setType(req.type() != null ? req.type() : "normal");
            cat.setDescription(req.description());
            cat.setSortNo(req.sortNo() != null ? req.sortNo() : 0);
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
        public void handle(HttpExchange ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var existing = catSvc.findById(id);
            if (existing.isEmpty()) { ctx.sendJson(404, ApiResponse.error("分类不存在")); return; }
            var cat = existing.get();
            // Null means "leave unchanged": a record cannot tell an absent
            // field from a null one, and refusing to clear is the safe side.
            var req = AdminBodies.validated(ctx, UpdateCategoryRequest.class);
            if (req.name() != null) cat.setName(req.name());
            if (req.parentId() != null) cat.setParentId(req.parentId());
            if (req.type() != null) cat.setType(req.type());
            if (req.description() != null) cat.setDescription(req.description());
            if (req.sortNo() != null) cat.setSortNo(req.sortNo());
            if (req.status() != null) cat.setStatus(req.status());
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
        public void handle(HttpExchange ctx) throws Exception {
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
