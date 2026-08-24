package com.jujin.point.domain.event;

/**
 * A user's mute deadline changed. {@code forbiddenUntil == 0} means the mute
 * was lifted. Wired to a system notification — without it the user only
 * discovers the mute through opaque request failures.
 */
public record UserForbiddenEvent(long userId, long forbiddenUntil, long timestamp)
    implements PointDomainEvent {}
