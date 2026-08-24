package com.jujin.point.domain.event;

/**
 * Published by MessageService after a notification row is actually written.
 * The WebSocket layer subscribes to this single event instead of re-resolving
 * recipients per business event — any future notification type gains push
 * for free, and a ping can only fire when the message truly exists.
 */
public record NotificationSentEvent(long fromUserId, long toUserId, long timestamp)
    implements PointDomainEvent {}
