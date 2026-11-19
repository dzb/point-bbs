package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;
import com.jujin.freeway.commons.validation.NotNull;
import com.jujin.freeway.commons.validation.Size;

import java.util.List;
import java.util.Map;

/**
 * Admin request bodies.
 *
 * <p>These used to be {@code ctx.bodyAsJson(Map.class)} with hand-rolled casts:
 * a wrong JSON type became a 500 and a missing field needed its own if. As
 * records they go through {@code BeanValidator}, and {@code PointErrorMapper}
 * turns a failure into 400 {@code validation_failed} with per-field details.</p>
 *
 * <p>Optional fields are boxed so "absent" stays distinguishable from a
 * legitimate zero — a category update must be able to leave {@code sortNo}
 * alone.</p>
 */
public interface AdminDtos {

    record CreateCategoryRequest(
        @NotBlank @Size(max = 32) String name,
        Long parentId,
        String type,
        @Size(max = 1024) String description,
        Integer sortNo
    ) {}

    /** Partial update: a null field is left unchanged. */
    record UpdateCategoryRequest(
        @Size(max = 32) String name,
        Long parentId,
        String type,
        @Size(max = 1024) String description,
        Integer sortNo,
        Integer status
    ) {}

    record SaveConfigsRequest(@NotNull Map<String, Object> configs) {}

    record SetConfigRequest(@NotNull Object value) {}

    record MarkMessagesReadRequest(@NotNull @Size(min = 1) List<Long> ids) {}
}
