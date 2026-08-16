package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;
import com.jujin.freeway.commons.validation.Size;

/**
 * Article-related request DTOs.
 */
public interface ArticleDtos {

    record CreateArticleRequest(
        @NotBlank @Size(max = 128) String title,
        @Size(max = 2000) String summary,
        @NotBlank @Size(max = 200_000) String content,
        String contentType,
        String cover,
        String sourceUrl,
        java.util.List<String> tags
    ) {}

    record UpdateArticleRequest(
        @Size(max = 128) String title,
        @Size(max = 2000) String summary,
        @Size(max = 200_000) String content,
        String contentType,
        String cover,
        String sourceUrl,
        java.util.List<String> tags
    ) {}
}
