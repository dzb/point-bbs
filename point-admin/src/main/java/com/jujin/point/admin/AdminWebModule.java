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
 * Deferred contributions use canonical ids (snake_name@package) so ordering
 * references stay stable instead of relying on string literals.
 *
 * Filter order:
 * 1. AuthFilter (from WebModule) — resolves CurrentUser
 * 2. AdminFilter — checks admin roles via the AuthApi served by the web layer
 */
public class AdminWebModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Admin authorization filter positioned after AuthFilter
        // addDeferred(Class) generates the canonical id "admin_filter@com.jujin.point.admin.filter";
        // after(...) points at AuthFilter's contribution id directly
        binder
            .contribute(HttpFilter.class)
            .addDeferred(AdminFilter.class)
            .after("auth_filter@com.jujin.point.web.filter");

        // Admin audit trail — AuthApi seam from the web layer, Orm from DbModule
        binder.bind(AdminAudit.class).to(AdminAudit.class);

        // Admin route groups
        binder.contribute(RouteGroup.class).add(AdminTopicRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminUserRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminCategoryRoutes.routes());
        binder.contribute(RouteGroup.class).add(AdminConfigRoutes.routes());
    }
}
