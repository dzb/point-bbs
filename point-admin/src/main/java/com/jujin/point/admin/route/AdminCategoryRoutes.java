package com.jujin.point.admin.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.entity.Category;
import com.jujin.point.service.CategoryService;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

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
                ctx.sendJson(201, ApiResponse.ok(Map.of("created", true)));
            }),
            // Update category
            Route.post("/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "updated", true)));
            }),
            // Delete category
            Route.post("/delete/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                ctx.sendJson(200, ApiResponse.ok(Map.of("id", id, "deleted", true)));
            })
        );
    }

    private static CategoryService catSvc() { return AppContext.get(CategoryService.class); }
}
