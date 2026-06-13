package com.jujin.point.domain.event;

public record UserFavoritedEvent(long userId, long entityId, String entityType, long timestamp)
    implements PointDomainEvent {}
