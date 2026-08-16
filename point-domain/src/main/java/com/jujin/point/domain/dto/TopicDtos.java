package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;
import com.jujin.freeway.commons.validation.Size;

/**
 * Topic-related request DTOs.
 */
public interface TopicDtos {

    record CreateTopicRequest(
        int type,
        Long categoryId,
        @Size(max = 128) String title,
        @NotBlank @Size(max = 100_000) String content,
        String contentType,
        java.util.List<String> tags,
        String imageList,
        String hideContent,
        CreateVoteRequest vote,
        int bountyScore
    ) {}

    record UpdateTopicRequest(
        @Size(max = 128) String title,
        @Size(max = 100_000) String content,
        String contentType,
        Long categoryId,
        java.util.List<String> tags,
        String hideContent
    ) {}

    record CreateVoteRequest(
        int type,
        String title,
        int voteNum,
        java.util.List<String> options,
        long expiredAt
    ) {}
}
