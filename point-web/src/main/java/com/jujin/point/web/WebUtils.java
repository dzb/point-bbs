package com.jujin.point.web;

import com.jujin.freeway.commons.validation.BeanValidator;
import com.jujin.freeway.http.HttpContext;
import com.jujin.freeway.http.ValidationException;

/** Shared helpers for route handlers. */
public final class WebUtils {
    private WebUtils() {}

    /** Read an integer query parameter, falling back to {@code defaultVal} when absent/invalid. */
    public static int intParam(HttpContext ctx, String name, int defaultVal) {
        return ctx.queryParam(name, Integer.class).orElse(defaultVal);
    }

    /**
     * Deserialize and validate a request body — the handler-class equivalent
     * of the typed {@code Route.post(path, Body.class, handler)} convenience,
     * which runs the same {@code bodyAsJson + BeanValidator} pair before the
     * handler body (validation failures surface as {@link ValidationException}
     * → the app's 400 error mapping).
     */
    public static <T> T validatedBody(HttpContext ctx, Class<T> bodyType)
        throws java.io.IOException {
        T body = ctx.bodyAsJson(bodyType);
        var result = BeanValidator.validate(body);
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        return body;
    }
}
