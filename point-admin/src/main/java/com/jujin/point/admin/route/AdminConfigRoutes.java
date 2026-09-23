package com.jujin.point.admin.route;

import com.jujin.point.admin.AdminAudit;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.service.SysConfigService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.util.Map;

public class AdminConfigRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/sys-config",
            // List all configs
            Route.get("", ListConfigsHandler.class),
            // Save all configs
            Route.post("", SaveConfigsHandler.class),
            // Get single config
            Route.get("/{key}", GetConfigHandler.class),
            // Set single config
            Route.post("/{key}", SetConfigHandler.class)
        );
    }

    public static final class ListConfigsHandler implements RouteHandler {
        private final SysConfigService configSvc;

        public ListConfigsHandler(SysConfigService configSvc) {
            this.configSvc = configSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var configs = configSvc.getAll();
            ctx.sendJson(200, ApiResponse.ok(configs));
        }
    }

    public static final class SaveConfigsHandler implements RouteHandler {
        private final SysConfigService configSvc;
        private final AdminAudit audit;

        public SaveConfigsHandler(SysConfigService configSvc, AdminAudit audit) {
            this.configSvc = configSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var req = ctx.bodyAsJson(Map.class);
            if (req == null) {
                ctx.sendJson(400, ApiResponse.error("请求体不能为空"));
                return;
            }
            @SuppressWarnings("unchecked")
            var configs = (Map<String, String>) req.get("configs");
            if (configs != null) {
                configs.forEach((k, v) -> {
                    configSvc.set(k, v);
                    audit.log(ctx, "update", "config", 0,
                        "修改配置: " + k);
                });
            }
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class GetConfigHandler implements RouteHandler {
        private final SysConfigService configSvc;

        public GetConfigHandler(SysConfigService configSvc) {
            this.configSvc = configSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            String key = ctx.pathVar("key").orElse(null);
            var value = configSvc.get(key);
            ctx.sendJson(200, ApiResponse.ok(Map.of("key", key, "value", value)));
        }
    }

    public static final class SetConfigHandler implements RouteHandler {
        private final SysConfigService configSvc;
        private final AdminAudit audit;

        public SetConfigHandler(SysConfigService configSvc, AdminAudit audit) {
            this.configSvc = configSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            String key = ctx.pathVar("key").orElse(null);
            var req = ctx.bodyAsJson(Map.class);
            if (req == null || req.get("value") == null) {
                ctx.sendJson(400, ApiResponse.error("缺少 value 参数"));
                return;
            }
            String value = String.valueOf(req.get("value"));
            configSvc.set(key, value);
            audit.log(ctx, "update", "config", 0,
                "修改配置: " + key);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }
}
