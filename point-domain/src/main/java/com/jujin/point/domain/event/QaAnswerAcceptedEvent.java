package com.jujin.point.domain.event;

public record QaAnswerAcceptedEvent(long userId, long topicId, long commentId, long timestamp)
    implements PointDomainEvent {}
