package com.jujin.point.domain.event;

/**
 * An operator (admin) soft-deleted a topic. Wired to a notification so the
 * author learns their post was removed — self-deletions skip the message
 * (the author obviously knows). Regular user deletes of their own topics
 * publish nothing.
 */
public record TopicDeletedEvent(long operatorId, long authorId, long topicId, long timestamp)
    implements PointDomainEvent {}
