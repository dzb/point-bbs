package com.jujin.point.domain.event;

public record UserMentionedEvent(long fromUserId, long mentionedUserId,
    String entityType, long entityId,
    String contentPreview, long timestamp)
    implements PointDomainEvent {}
