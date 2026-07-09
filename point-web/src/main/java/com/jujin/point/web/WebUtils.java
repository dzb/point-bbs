package com.jujin.point.web;

import com.jujin.freeway.http.HttpContext;

/** Shared helpers for route handlers. */
public final class WebUtils {
    private WebUtils() {}

    /** Read an integer query parameter, falling back to {@code defaultVal} when absent/invalid. */
    public static int intParam(HttpContext ctx, String name, int defaultVal) {
        return ctx.queryParam(name, Integer.class).orElse(defaultVal);
    }
}
