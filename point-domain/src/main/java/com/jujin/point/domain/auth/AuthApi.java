package com.jujin.point.domain.auth;

import com.jujin.point.domain.dto.CurrentUser;

/**
 * Session-identity queries served by the web layer ({@code AuthRpc}) as a
 * container-bound service.
 *
 * Declared here so point-admin can ask "who is calling?" without a
 * compile-time dependency on point-web: the provider reads AuthFilter's
 * ScopedValue-bound CurrentUser inline on the requesting thread. No default
 * fallbacks on {@link #current()} on purpose — if the web module is not
 * installed, resolution fails loudly instead of silently treating every
 * request as anonymous.
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
