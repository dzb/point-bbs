package com.jujin.point.domain.event;

public record UserFollowedEvent(long userId, long otherId, long timestamp)
    implements PointDomainEvent {}
