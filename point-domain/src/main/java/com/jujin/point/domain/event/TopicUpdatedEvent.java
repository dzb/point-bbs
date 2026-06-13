package com.jujin.point.domain.event;

public record TopicUpdatedEvent(long userId, long topicId, long timestamp)
    implements PointDomainEvent {}
