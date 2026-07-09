package com.jujin.point.admin.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.entity.Category;
import com.jujin.point.service.CategoryService;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.util.Map;

public class AdminCategoryRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/category",
            // List all categories
            Route.get("", ctx -> {
                var tree = catSvc().getTree();
                ctx.sendJson(200, ApiResponse.ok(tree));
            }),
            // Create category
            Route.post("", ctx -> {
                var req = ctx.bodyAsJson(Map.class);
                var cat = new Category();
                cat.setName((String) req.get("name"));
                cat.setParentId(req.get("parentId") != null ? ((Number) req.get("parentId")).longValue() : 0L);
                cat.setType((String) req.getOrDefault("type", "normal"));
                cat.setDescription((String) req.get("description"));
                cat.setSortNo(req.get("sortNo") != null ? ((Number) req.get("sortNo")).intValue() : 0);
                cat.setStatus(1);
                cat.setCreateTime(System.currentTimeMillis());
                catSvc().create(cat);
                ctx.sendJson(201, ApiResponse.ok(Map.of("created", true, "id", cat.getId())));
            }),
            // Update category
            Route.post("/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                var existing = catSvc().findById(id);
                if (existing.isEmpty()) { ctx.sendJson(404, ApiResponse.error("分类不存在")); return; }
                var cat = existing.get();
                var req = ctx.bodyAsJson(Map.class);
                if (req.containsKey("name")) cat.setName((String) req.get("name"));
                if (req.containsKey("parentId")) cat.setParentId(((Number) req.get("parentId")).longValue());
                if (req.containsKey("type")) cat.setType((String) req.get("type"));
                if (req.containsKey("description")) cat.setDescription((String) req.get("description"));
                if (req.containsKey("sortNo")) cat.setSortNo(((Number) req.get("sortNo")).intValue());
                if (req.containsKey("status")) cat.setStatus(((Number) req.get("status")).intValue());
                catSvc().update(cat);
                ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "updated", true)));
            }),
            // Delete category (soft-delete)
            Route.post("/delete/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class).orElse(0L);
                var existing = catSvc().findById(id);
                if (existing.isEmpty()) { ctx.sendJson(404, ApiResponse.error("分类不存在")); return; }
                catSvc().delete(id);
                ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "deleted", true)));
            })
        );
    }

    private static CategoryService catSvc() { return AppContext.get(CategoryService.class); }
}
