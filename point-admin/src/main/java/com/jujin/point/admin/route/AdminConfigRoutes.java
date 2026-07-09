package com.jujin.point.admin.route;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.SysConfigService;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;

import java.util.Map;

public class AdminConfigRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/sys-config",
            // List all configs
            Route.get("", ctx -> {
                var configs = configSvc().getAll();
                ctx.sendJson(200, ApiResponse.ok(configs));
            }),
            // Save all configs
            Route.post("", ctx -> {
                var req = ctx.bodyAsJson(Map.class);
                @SuppressWarnings("unchecked")
                var configs = (Map<String, String>) req.get("configs");
                if (configs != null) {
                    configs.forEach((k, v) -> configSvc().set(k, v));
                }
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Get single config
            Route.get("/{key}", ctx -> {
                String key = ctx.pathVar("key").orElse(null);
                var value = configSvc().get(key);
                ctx.sendJson(200, ApiResponse.ok(Map.of("key", key, "value", value)));
            }),
            // Set single config
            Route.post("/{key}", ctx -> {
                String key = ctx.pathVar("key").orElse(null);
                var req = ctx.bodyAsJson(Map.class);
                String value = (String) req.get("value");
                configSvc().set(key, value);
                ctx.sendJson(200, ApiResponse.ok());
            })
        );
    }

    private static SysConfigService configSvc() { return AppContext.get(SysConfigService.class); }
}
