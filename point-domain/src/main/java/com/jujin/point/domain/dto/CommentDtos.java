package com.jujin.point.domain.dto;

/**
 * Comment-related DTOs.
 */
public interface CommentDtos {

    record CreateCommentRequest(
        String entityType,
        Long entityId,
        String content,
        String contentType,
        String imageList,
        Long quoteId
    ) {}
}
