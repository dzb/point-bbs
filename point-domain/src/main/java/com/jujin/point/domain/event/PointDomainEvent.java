package com.jujin.point.domain.event;

/**
 * Sealed interface for all BBS domain events.
 */
public sealed interface PointDomainEvent
    permits TopicCreatedEvent, TopicUpdatedEvent, TopicDeletedEvent, TopicRecommendedEvent,
            CommentCreatedEvent, UserLikedEvent, UserUnlikedEvent,
            UserFollowedEvent, UserUnfollowedEvent, UserFavoritedEvent,
            CheckInEvent, UserLoginEvent, LevelUpEvent, BadgeGrantedEvent,
            QaAnswerAcceptedEvent, UserMentionedEvent {

    long timestamp();
}
