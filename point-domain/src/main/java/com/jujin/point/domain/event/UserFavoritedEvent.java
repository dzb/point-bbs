package com.jujin.point.domain.event;

/** A user bookmarked an entity. Unlike/unfavorite is intentionally silent. */
public record UserFavoritedEvent(long userId, long entityId, String entityType, long timestamp)
    implements PointDomainEvent {}
