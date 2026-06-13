package com.jujin.point.domain.dto;

import java.util.List;

/**
 * Generic paginated result.
 */
public record PageResult<T>(
    List<T> items,
    int page,
    int pageSize,
    long total
) {
    public int totalPages() {
        return pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }

    public boolean hasMore() {
        return page < totalPages();
    }
}
