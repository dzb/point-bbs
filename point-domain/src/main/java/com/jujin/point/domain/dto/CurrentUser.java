package com.jujin.point.domain.dto;

/**
 * Current user record — resolved from JWT token in AuthFilter via ScopedValue.
 */
public record CurrentUser(long userId, String nickname, String avatar, java.util.Set<String> roles) {
    public CurrentUser {
        roles = roles == null ? java.util.Set.of() : java.util.Set.copyOf(roles);
    }

    public boolean isAdmin() {
        return roles.contains("admin") || roles.contains("owner");
    }
}
