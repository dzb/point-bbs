package com.jujin.point.admin;

import com.jujin.freeway.commons.validation.BeanValidator;
import com.jujin.freeway.commons.validation.ValidationError;
import com.jujin.freeway.commons.validation.ValidationResult;
import com.jujin.freeway.http.HttpExchange;
import com.jujin.freeway.http.ValidationException;
import com.jujin.point.domain.dto.DtoErrors;

import java.util.Map;

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
            throw new ValidationException(DtoErrors.malformedBody(malformed));
        }
        var result = BeanValidator.validate(body);
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
        return body;
    }

    /**
     * Rejects {@code null} map values with a per-key field error. Bean
     * validation covers the map itself, not its entries; letting a null
     * through would persist the literal string {@code "null"}.
     */
    public static void requireNonNullValues(Map<String, ?> values, String fieldPrefix) {
        var errors = values.entrySet().stream()
            .filter(entry -> entry.getValue() == null)
            .map(entry -> new ValidationError(
                fieldPrefix + "." + entry.getKey(), "must not be null", null))
            .toList();
        if (!errors.isEmpty()) {
            throw new ValidationException(ValidationResult.of(errors));
        }
    }
}
