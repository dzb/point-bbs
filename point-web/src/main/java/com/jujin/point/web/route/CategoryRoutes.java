package com.jujin.point.web.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.CategoryService;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

public class CategoryRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/categories",
            Route.get("", ctx -> {
                var cats = svc().findAllActive();
                ctx.sendJson(200, ApiResponse.ok(cats));
            })
        );
    }

    private static CategoryService svc() { return AppContext.get(CategoryService.class); }
}
