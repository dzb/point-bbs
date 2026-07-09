package com.jujin.point.domain.dto;

import java.util.Set;

/**
 * Current user record — resolved from JWT token in AuthFilter via ScopedValue.
 */
public record CurrentUser(
    long userId,
    String nickname,
    String avatar,
    Set<String> roles
) {
    public CurrentUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.contains("admin") || roles.contains("owner");
    }
}
