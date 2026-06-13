package com.jujin.point.domain.event;

public record TopicRecommendedEvent(long topicId, boolean recommend, long timestamp)
    implements PointDomainEvent {}
