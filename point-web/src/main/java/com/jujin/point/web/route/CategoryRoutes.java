package com.jujin.point.web.route;

import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.CategoryService;

public class CategoryRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/categories",
            Route.get("", ListCategoriesHandler.class)
        );
    }

    public static final class ListCategoriesHandler implements RouteHandler {
        private final CategoryService svc;

        public ListCategoriesHandler(CategoryService svc) {
            this.svc = svc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var cats = svc.findAllActive();
            ctx.sendJson(200, ApiResponse.ok(cats));
        }
    }
}
