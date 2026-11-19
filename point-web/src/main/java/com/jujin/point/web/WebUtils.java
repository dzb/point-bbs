package com.jujin.point.web;

import com.jujin.freeway.commons.validation.BeanValidator;
import com.jujin.freeway.commons.validation.ValidationError;
import com.jujin.freeway.commons.validation.ValidationResult;
import com.jujin.freeway.http.HttpExchange;
import com.jujin.freeway.http.ValidationException;

import java.util.List;

/** Shared helpers for route handlers. */
public final class WebUtils {
    private WebUtils() {}

    /** Read an integer query parameter, falling back to {@code defaultVal} when absent/invalid. */
    public static int intParam(HttpExchange ctx, String name, int defaultVal) {
        return ctx.queryParam(name, Integer.class).orElse(defaultVal);
    }

    /**
     * Deserialize and validate a request body — the handler-class equivalent
     * of the typed {@code Route.post(path, Body.class, handler)} convenience,
     * which runs the same {@code bodyAsJson + BeanValidator} pair before the
     * handler body (validation failures surface as {@link ValidationException}
     * → the app's 400 error mapping).
     *
     * <p>A body the codec cannot coerce into the DTO (wrong JSON type, unknown
     * shape) is a client error too: it becomes the same 400 instead of a 500
     * from an {@link IllegalArgumentException} escaping the decoder.</p>
     */
    public static <T> T validatedBody(HttpExchange ctx, Class<T> bodyType)
        throws java.io.IOException {
        T body;
        try {
            body = ctx.bodyAsJson(bodyType);
        } catch (IllegalArgumentException malformed) {
            throw invalidBody(malformed);
        }
        var result = BeanValidator.validate(body);
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        return body;
    }

    /** 400 for a body the decoder rejected before any validation ran. */
    public static ValidationException invalidBody(IllegalArgumentException cause) {
        return new ValidationException(ValidationResult.of(List.of(
            new ValidationError("body", "请求体格式或类型不正确", cause.getMessage())
        )));
    }
}
