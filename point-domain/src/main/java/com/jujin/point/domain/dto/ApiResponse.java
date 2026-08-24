package com.jujin.point.domain.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified API response envelope.
 */
public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    /**
     * Canonical list-envelope payload: {items, page, pageSize, total} in wire
     * order. Replaces the hand-rolled LinkedHashMap previously duplicated at
     * every list endpoint.
     */
    public static Map<String, Object> page(List<?> items, int page, int pageSize, long total) {
        var m = new LinkedHashMap<String, Object>();
        m.put("items", items);
        m.put("page", page);
        m.put("pageSize", pageSize);
        m.put("total", total);
        return m;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(-1, message, null);
    }
}
