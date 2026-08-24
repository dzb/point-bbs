package com.jujin.point.service;

import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.EventSubscriber;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.db.repository.*;
import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.eventhandler.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Service module — binds all business services and event subscribers.
 *
 * SINGLETON is the default scope, so explicit .scope() is omitted.
 * Subscribers resolve NotificationHandler via AppContext (lambdas have no
 * container handle at bind time) and are best-effort: a failure in a
 * notification must never roll back the business operation that published
 * the event.
 */
public class ServiceModule implements ModuleEx {
    private static final Logger log = LoggerFactory.getLogger(ServiceModule.class);

    @Override
    public void bind(Binder binder) {
        // Repositories
        binder.bind(UserRepository.class).to(UserRepository.class);
        binder.bind(TopicRepository.class).to(TopicRepository.class);
        binder.bind(CommentRepository.class).to(CommentRepository.class);

        // Core services
        binder.bind(SysConfigService.class).to(SysConfigService.class);
        binder.bind(UserService.class).to(UserService.class);
        binder.bind(TopicService.class).to(TopicService.class);
        binder.bind(CommentService.class).to(CommentService.class);
        binder.bind(ArticleService.class).to(ArticleService.class);
        binder.bind(CategoryService.class).to(CategoryService.class);
        binder.bind(TagService.class).to(TagService.class);

        // Interaction services
        binder.bind(UserLikeService.class).to(UserLikeService.class);
        binder.bind(FavoriteService.class).to(FavoriteService.class);
        binder.bind(UserFollowService.class).to(UserFollowService.class);
        binder.bind(MessageService.class).to(MessageService.class);
        binder.bind(MentionNotifier.class).to(MentionNotifier.class);

        // Permission & RBAC
        binder.bind(PermissionService.class).to(PermissionService.class);

        // Upload
        binder.bind(UploadService.class).to(UploadService.class);

        // OAuth
        binder.bind(GitHubOAuthProvider.class).to(GitHubOAuthProvider.class);

        // Auth
        binder.bind(AuthService.class).to(AuthService.class);

        binder.bind(NotificationHandler.class).to(NotificationHandler.class);

        var handlers = binder.contribute(EventSubscriber.class);
        handlers.add("notify-comment", bestEffort(CommentCreatedEvent.class,
            "comment notification", e -> handler().onCommentCreated(e)));
        handlers.add("notify-like", bestEffort(UserLikedEvent.class,
            "like notification", e -> handler().onUserLiked(e)));
        handlers.add("notify-favorite", bestEffort(UserFavoritedEvent.class,
            "favorite notification", e -> handler().onUserFavorited(e)));
        handlers.add("notify-follow", bestEffort(UserFollowedEvent.class,
            "follow notification", e -> handler().onUserFollowed(e)));
        handlers.add("notify-mention", bestEffort(UserMentionedEvent.class,
            "mention notification", e -> handler().onUserMentioned(e)));
        handlers.add("notify-qa-accepted", bestEffort(QaAnswerAcceptedEvent.class,
            "qa-accepted notification", e -> handler().onQaAnswerAccepted(e)));
        handlers.add("notify-topic-deleted", bestEffort(TopicDeletedEvent.class,
            "topic-deleted notification", e -> handler().onTopicDeleted(e)));
        handlers.add("notify-forbidden", bestEffort(UserForbiddenEvent.class,
            "forbidden notification", e -> handler().onUserForbidden(e)));
    }

    private static NotificationHandler handler() {
        return AppContext.get(NotificationHandler.class);
    }

    /** Wraps a handler call as an isolated subscriber: failures log, never propagate. */
    private static <E extends PointDomainEvent> EventSubscriber<E> bestEffort(
        Class<E> type, String what, Consumer<E> call) {
        return EventSubscriber.of(type, e -> {
            try {
                call.accept(e);
            } catch (Exception ex) {
                log.warn("{} failed", what, ex);
            }
        });
    }
}
