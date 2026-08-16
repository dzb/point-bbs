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
                if (req == null) {
                    ctx.sendJson(400, ApiResponse.error("请求体不能为空"));
                    return;
                }
                @SuppressWarnings("unchecked")
                var configs = (Map<String, String>) req.get("configs");
                if (configs != null) {
                    configs.forEach((k, v) -> {
                    configSvc().set(k, v);
                    com.jujin.point.admin.AdminAudit.log(ctx, "update", "config", 0,
                        "修改配置: " + k);
                });
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
                if (req == null || req.get("value") == null) {
                    ctx.sendJson(400, ApiResponse.error("缺少 value 参数"));
                    return;
                }
                String value = String.valueOf(req.get("value"));
                configSvc().set(key, value);
                com.jujin.point.admin.AdminAudit.log(ctx, "update", "config", 0,
                    "修改配置: " + key);
                ctx.sendJson(200, ApiResponse.ok());
            })
        );
    }

    private static SysConfigService configSvc() { return AppContext.get(SysConfigService.class); }
}
