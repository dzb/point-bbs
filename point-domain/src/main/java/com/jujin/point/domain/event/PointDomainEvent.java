package com.jujin.point.domain.event;

/**
 * Sealed interface for all BBS domain events.
 */
public sealed interface PointDomainEvent
    permits CommentCreatedEvent, TopicDeletedEvent, UserLikedEvent, UserFavoritedEvent,
            UserFollowedEvent, UserForbiddenEvent,
            QaAnswerAcceptedEvent, UserMentionedEvent, NotificationSentEvent {

    long timestamp();
}
