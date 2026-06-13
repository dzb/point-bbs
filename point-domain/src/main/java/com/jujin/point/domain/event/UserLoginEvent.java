package com.jujin.point.domain.event;

public record UserLoginEvent(long userId, long loginTime, boolean isNewLogin, long timestamp)
    implements PointDomainEvent {}
