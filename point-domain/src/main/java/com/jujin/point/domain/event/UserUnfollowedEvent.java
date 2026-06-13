package com.jujin.point.domain.event;

public record UserUnfollowedEvent(long userId, long otherId, long timestamp)
    implements PointDomainEvent {}
