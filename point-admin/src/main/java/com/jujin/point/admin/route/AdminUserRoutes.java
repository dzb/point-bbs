package com.jujin.point.admin.route;

import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.dto.ApiResponse;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.User;
import com.jujin.point.service.UserService;
import com.jujin.freeway.http.Route;
import com.jujin.freeway.http.RouteGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUserRoutes {

    public static RouteGroup routes() {
        return RouteGroup.of("/api/admin/user",
            // List users (paginated)
            Route.get("", ctx -> {
                int page = parseInt(ctx.queryParam("page"), 1);
                int pageSize = parseInt(ctx.queryParam("pageSize"), 20);
                var repo = userRepo();
                var users = repo.findPage(page, pageSize);
                users.forEach(u -> u.setPassword(null));
                var result = Map.of("items", (Object) users, "total", repo.countAll(),
                    "page", page, "pageSize", pageSize);
                ctx.sendJson(200, ApiResponse.ok(result));
            }),
            // Get user detail
            Route.get("/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                var user = userSvc().findById(id);
                user.ifPresent(u -> u.setPassword(null));
                ctx.sendJson(200, ApiResponse.ok(user.orElse(null)));
            }),
            // Update user
            Route.post("/update/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                var req = ctx.bodyAsJson(UpdateUserRequest.class);
                userSvc().updateUser(id, req);
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Forbid user (temporary ban)
            Route.post("/forbidden/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                var user = userSvc().findById(id).orElse(null);
                if (user != null) {
                    user.setForbiddenEndTime(System.currentTimeMillis() + 365L * 86400 * 1000);
                    userSvc().updateUser(id, new UpdateUserRequest(
                        user.getNickname(), user.getAvatar(), user.getGender(),
                        user.getDescription(), user.getHomePage(), user.getBackgroundImage()
                    ));
                }
                ctx.sendJson(200, ApiResponse.ok());
            }),
            // Unforbid user
            Route.post("/unforbidden/{id}", ctx -> {
                long id = ctx.pathVar("id", Long.class);
                var user = userSvc().findById(id).orElse(null);
                if (user != null) {
                    user.setForbiddenEndTime(0);
                    userSvc().updateUser(id, new UpdateUserRequest(
                        user.getNickname(), user.getAvatar(), user.getGender(),
                        user.getDescription(), user.getHomePage(), user.getBackgroundImage()
                    ));
                }
                ctx.sendJson(200, ApiResponse.ok());
            })
        );
    }

    private static UserService userSvc() { return AppContext.get(UserService.class); }
    private static UserRepository userRepo() { return AppContext.get(UserRepository.class); }

    private static int parseInt(String s, int def) {
        if (s == null || s.isEmpty()) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
