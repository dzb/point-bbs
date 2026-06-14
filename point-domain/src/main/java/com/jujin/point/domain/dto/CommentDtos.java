package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;

/**
 * Comment-related DTOs.
 */
public interface CommentDtos {

    record CreateCommentRequest(
        String entityType,
        Long entityId,
        @NotBlank String content,
        String contentType,
        String imageList,
        Long quoteId
    ) {}
}
