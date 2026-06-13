package com.jujin.point.domain.event;

public record UserUnlikedEvent(long userId, long entityId, String entityType, long timestamp)
    implements PointDomainEvent {}
