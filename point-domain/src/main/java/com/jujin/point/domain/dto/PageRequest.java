package com.jujin.point.domain.dto;

/**
 * Standard pagination request.
 */
public record PageRequest(int page, int pageSize) {
    public PageRequest {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;
    }

    public static PageRequest of(int page, int pageSize) {
        return new PageRequest(page, pageSize);
    }

    public long offset() {
        return (long) (page - 1) * pageSize;
    }
}
