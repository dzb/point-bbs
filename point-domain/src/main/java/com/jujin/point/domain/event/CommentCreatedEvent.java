package com.jujin.point.domain.event;

public record CommentCreatedEvent(long userId, long commentId, long entityId, String entityType, long timestamp)
    implements PointDomainEvent {}
