package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;
import com.jujin.freeway.commons.validation.Size;

/**
 * Comment-related DTOs.
 */
public interface CommentDtos {

    record CreateCommentRequest(
        String entityType,
        Long entityId,
        @NotBlank @Size(max = 20_000) String content,
        String contentType,
        String imageList,
        Long quoteId
    ) {}
}
