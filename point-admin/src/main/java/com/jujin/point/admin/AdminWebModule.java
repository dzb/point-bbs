package com.jujin.point.admin;

import com.jujin.freeway.http.filter.HttpFilter;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.admin.filter.AdminFilter;
import com.jujin.point.admin.route.*;

/**
 * Admin web module — contributes admin API routes with auth + admin filtering.
 *
 * Freeway 1.3.2: add(Class) generates a canonical auto-ID (snake_name@package),
 * and before/after ordering uses these stable IDs instead of fragile string literals.
 *
 * Filter order:
 * 1. AuthFilter (from WebModule) — resolves CurrentUser
 * 2. AdminFilter — checks admin roles
 */
public class AdminWebModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Admin authorization filter positioned after AuthFilter
        // add(Class) generates stable canonical ID: "admin_filter@com.jujin.point.admin.filter"
        // after(AuthFilter.class) references AuthFilter's canonical ID directly
        binder
            .contribute(HttpFilter.class)
            .add(AdminFilter.class)
            .after("auth_filter@com.jujin.point.web.filter");

        // Admin route groups
        binder.contribute(RouteGroup.class).add(AdminTopicRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminUserRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminCategoryRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminConfigRoutes.routes());
    }
}
