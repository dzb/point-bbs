package com.jujin.point.admin.route;

import com.jujin.point.admin.AdminAudit;
import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.service.UserService;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.route.Route;
import com.jujin.freeway.http.route.RouteGroup;
import com.jujin.freeway.http.route.RouteHandler;

import java.util.Map;

public class AdminUserRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/user",
            // List users (paginated)
            Route.get("", ListUsersHandler.class),
            // Get user detail
            Route.get("/{id}", UserDetailHandler.class),
            // Update user
            Route.post("/update/{id}", UpdateUserHandler.class),
            // Forbid user (temporary ban)
            Route.post("/forbidden/{id}", ForbiddenUserHandler.class),
            // Unforbid user
            Route.post("/unforbidden/{id}", UnforbiddenUserHandler.class)
        );
    }

    public static final class ListUsersHandler implements RouteHandler {
        private final UserRepository userRepo;

        public ListUsersHandler(UserRepository userRepo) {
            this.userRepo = userRepo;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            var pr = PageRequest.of(
                ctx.queryParam("page", Integer.class).orElse(1),
                ctx.queryParam("pageSize", Integer.class).orElse(20)
            );
            var users = userRepo.findPage(pr.page(), pr.pageSize());
            users.forEach(u -> u.setPassword(null));
            var result = Map.of("items", (Object) users, "total", userRepo.countAll(),
                "page", pr.page(), "pageSize", pr.pageSize());
            ctx.sendJson(200, ApiResponse.ok(result));
        }
    }

    public static final class UserDetailHandler implements RouteHandler {
        private final UserService userSvc;

        public UserDetailHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var user = userSvc.findById(id);
            user.ifPresent(u -> u.setPassword(null));
            ctx.sendJson(200, ApiResponse.ok(user.orElse(null)));
        }
    }

    public static final class UpdateUserHandler implements RouteHandler {
        private final UserService userSvc;

        public UpdateUserHandler(UserService userSvc) {
            this.userSvc = userSvc;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            var req = ctx.bodyAsJson(UpdateUserRequest.class);
            userSvc.updateUser(id, req);
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class ForbiddenUserHandler implements RouteHandler {
        private final UserService userSvc;
        private final AdminAudit audit;

        public ForbiddenUserHandler(UserService userSvc, AdminAudit audit) {
            this.userSvc = userSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            if (userSvc.findById(id).isEmpty()) {
                ctx.sendJson(404, ApiResponse.error("用户不存在"));
                return;
            }
            userSvc.setForbiddenEndTime(id, System.currentTimeMillis() + 365L * 86400 * 1000);
            audit.log(ctx, "forbid", "user", id, "禁言用户一年");
            ctx.sendJson(200, ApiResponse.ok());
        }
    }

    public static final class UnforbiddenUserHandler implements RouteHandler {
        private final UserService userSvc;
        private final AdminAudit audit;

        public UnforbiddenUserHandler(UserService userSvc, AdminAudit audit) {
            this.userSvc = userSvc;
            this.audit = audit;
        }

        @Override
        public void handle(HttpContext ctx) throws Exception {
            long id = ctx.pathVar("id", Long.class).orElse(0L);
            if (userSvc.findById(id).isEmpty()) {
                ctx.sendJson(404, ApiResponse.error("用户不存在"));
                return;
            }
            userSvc.setForbiddenEndTime(id, 0);
            audit.log(ctx, "unforbid", "user", id, "解除禁言");
            ctx.sendJson(200, ApiResponse.ok());
        }
    }
}
