package com.jujin.point.web.filter;

/**
 * Authentication exception — thrown when auth is required but missing/invalid.
 */
public class AuthException extends RuntimeException {
    private final int statusCode;

    public AuthException(String message) {
        this(message, 401);
    }

    public AuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
}
