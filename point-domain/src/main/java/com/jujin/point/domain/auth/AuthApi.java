package com.jujin.point.domain.auth;

import com.jujin.point.domain.dto.CurrentUser;

/**
 * Session-identity queries served by the web layer over the CallBus
 * ({@code register("auth", ...)} / {@code consumer("auth", AuthApi.class)}).
 *
 * Declared here so point-admin can ask "who is calling?" without a
 * compile-time dependency on point-web: dispatch is inline on the requesting
 * thread, so the provider simply reads AuthFilter's ScopedValue-bound
 * CurrentUser. No default fallbacks on {@link #current()} on purpose — if the
 * bus or provider is missing, calls fail loudly instead of silently treating
 * every request as anonymous.
 */
public interface AuthApi {

    /** The authenticated user, or null when anonymous. */
    CurrentUser current();

    default boolean isAdmin() {
        var user = current();
        return user != null && user.isAdmin();
    }

    default long currentUserId() {
        var user = current();
        return user != null ? user.userId() : 0L;
    }
}
