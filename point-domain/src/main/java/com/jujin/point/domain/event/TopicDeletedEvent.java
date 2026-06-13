package com.jujin.point.domain.event;

public record TopicDeletedEvent(long userId, long topicId, long deleteUserId, long timestamp)
    implements PointDomainEvent {}
