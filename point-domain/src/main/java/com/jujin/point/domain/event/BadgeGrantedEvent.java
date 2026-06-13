package com.jujin.point.domain.event;

public record BadgeGrantedEvent(long userId, long badgeId, String badgeName, long timestamp)
    implements PointDomainEvent {}
