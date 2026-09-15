package com.jujin.point.web;

import com.jujin.point.domain.auth.AuthApi;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.web.filter.AuthFilter;

/**
 * Provider for {@link AuthApi} — answers session-identity questions
 * from the ScopedValue that AuthFilter binds per request. Container dispatch
 * is inline on the caller's thread, so this reads the requesting user directly.
 */
public class AuthRpc implements AuthApi {

    @Override
    public CurrentUser current() {
        return AuthFilter.currentUser();
    }
}
