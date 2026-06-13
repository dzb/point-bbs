package com.jujin.point.domain.event;

public record TopicCreatedEvent(long userId, long topicId, int topicType, long timestamp)
    implements PointDomainEvent {}
