package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.ValidationError;
import com.jujin.freeway.commons.validation.ValidationResult;

import java.util.List;

/**
 * The one definition of "the request body could not be decoded".
 *
 * <p>A body the JSON codec rejects (wrong type, unknown shape) is a client
 * error. Both web layers turn it into the same 400 {@code validation_failed}
 * envelope from here, so the field name and message cannot drift between the
 * public API and the admin API.</p>
 */
public final class DtoErrors {

    private DtoErrors() {}

    public static ValidationResult malformedBody(IllegalArgumentException cause) {
        return ValidationResult.of(List.of(
            new ValidationError("body", "请求体格式或类型不正确", cause.getMessage())
        ));
    }
}
