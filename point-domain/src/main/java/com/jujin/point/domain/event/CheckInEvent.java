package com.jujin.point.domain.event;

public record CheckInEvent(long userId, int consecutiveDays, long timestamp)
    implements PointDomainEvent {}
