package com.jujin.point.domain.event;

public record LevelUpEvent(long userId, int oldLevel, int newLevel, long timestamp)
    implements PointDomainEvent {}
