package com.jujin.point.domain.event;

public record UserLikedEvent(long userId, long entityId, String entityType, long timestamp)
    implements PointDomainEvent {}
