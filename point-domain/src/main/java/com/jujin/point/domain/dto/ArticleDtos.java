package com.jujin.point.domain.dto;

/**
 * Article-related request DTOs.
 */
public interface ArticleDtos {

    record CreateArticleRequest(
        String title,
        String summary,
        String content,
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
