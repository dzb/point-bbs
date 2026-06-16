package com.jujin.point.admin;

import com.jujin.point.admin.filter.AdminFilter;
import com.jujin.point.admin.route.*;
import com.jujin.freeway.http.HttpFilter;
import com.jujin.freeway.http.RouteGroup;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Module2;

/**
 * Admin web module — contributes admin API routes with auth + admin filtering.
 *
 * Filters are ordered by freeway's Extension sorting:
 * 1. AuthFilter (from WebModule) — resolves CurrentUser
 * 2. AdminFilter — checks admin roles
 */
public class AdminWebModule implements Module2 {

    @Override
    public void bind(Binder binder) {
        // Admin authorization filter (after auth)
        binder.contribute(HttpFilter.class)
            .add("admin-filter", new AdminFilter())
            .after("auth-filter");

        // Admin route groups
        binder.contribute(RouteGroup.class).add(AdminTopicRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminUserRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminCategoryRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminConfigRoutes.routes());
    }
}
