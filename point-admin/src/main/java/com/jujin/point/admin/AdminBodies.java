package com.jujin.point.admin;

import com.jujin.freeway.commons.validation.BeanValidator;
import com.jujin.freeway.commons.validation.ValidationError;
import com.jujin.freeway.commons.validation.ValidationResult;
import com.jujin.freeway.http.HttpExchange;
import com.jujin.freeway.http.ValidationException;

import java.util.List;

/**
 * Typed, validated request bodies for admin routes.
 *
 * <p>Same contract as {@code WebUtils.validatedBody} in point-web: parse into
 * a record, run {@code BeanValidator}, and turn decoder rejections into the
 * same 400 {@code validation_failed} envelope {@code PointErrorMapper}
 * produces. Duplicated as a small helper rather than adding a point-web
 * dependency to this module — the two web layers stay siblings, joined only by
 * point-domain's {@code AuthApi} seam.</p>
 */
public final class AdminBodies {

    private AdminBodies() {}

    public static <T> T validated(HttpExchange ctx, Class<T> bodyType) throws java.io.IOException {
        T body;
        try {
            body = ctx.bodyAsJson(bodyType);
        } catch (IllegalArgumentException malformed) {
            // Wrong JSON type (e.g. "abc" for a Long) is a client error, not a 500.
            throw new ValidationException(ValidationResult.of(List.of(
                new ValidationError("body", "请求体格式或类型不正确", malformed.getMessage())
            )));
        }
        var result = BeanValidator.validate(body);
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        return body;
    }
}
