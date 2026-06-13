package com.jujin.point.service;

/**
 * OAuth provider interface — strategy pattern for third-party login.
 */
public interface OAuthProvider {
    /** Provider name: github, google, wechat */
    String name();

    /** Build the authorization URL for redirecting the user. */
    String authorizeUrl(String state);

    /** Exchange authorization code for OAuth user info. */
    OAuthUserInfo handleCallback(String code);

    record OAuthUserInfo(String openId, String nickname, String avatar, String extraData) {}
}
