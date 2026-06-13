package com.jujin.point.domain.dto;

/**
 * Topic-related request DTOs.
 */
public interface TopicDtos {

    record CreateTopicRequest(
        int type,
        Long categoryId,
        String title,
        String content,
        String contentType,
        java.util.List<String> tags,
        String imageList,
        String hideContent,
        CreateVoteRequest vote,
        int bountyScore
    ) {}

    record UpdateTopicRequest(
        String title,
        String content,
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
