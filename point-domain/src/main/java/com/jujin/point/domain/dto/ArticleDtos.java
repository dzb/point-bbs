package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;

/**
 * Article-related request DTOs.
 */
public interface ArticleDtos {

    record CreateArticleRequest(
        @NotBlank String title,
        String summary,
        @NotBlank String content,
        String contentType,
        String cover,
        String sourceUrl,
        java.util.List<String> tags
    ) {}

    record UpdateArticleRequest(
        String title,
        String summary,
        String content,
        String contentType,
        String cover,
        String sourceUrl,
        java.util.List<String> tags
    ) {}
}
