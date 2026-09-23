package com.jujin.point.service;

import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.EventSubscriber;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.point.db.repository.*;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.eventhandler.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Service module — binds all business services and event subscribers.
 *
 * SINGLETON is the default scope, so explicit .scope() is omitted.
 * Subscribers are built through the contribution factory
 * ({@code add(id, Function<Container, T>)}), which receives the container
 * and resolves NotificationHandler once at startup — a missing binding
 * fails there, not at event time. They are best-effort: a failure in a
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
        handlers.add("notify-comment", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(CommentCreatedEvent.class,
                "comment notification", handler::onCommentCreated);
        });
        handlers.add("notify-like", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(UserLikedEvent.class,
                "like notification", handler::onUserLiked);
        });
        handlers.add("notify-favorite", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(UserFavoritedEvent.class,
                "favorite notification", handler::onUserFavorited);
        });
        handlers.add("notify-follow", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(UserFollowedEvent.class,
                "follow notification", handler::onUserFollowed);
        });
        handlers.add("notify-mention", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(UserMentionedEvent.class,
                "mention notification", handler::onUserMentioned);
        });
        handlers.add("notify-qa-accepted", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(QaAnswerAcceptedEvent.class,
                "qa-accepted notification", handler::onQaAnswerAccepted);
        });
        handlers.add("notify-topic-deleted", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(TopicDeletedEvent.class,
                "topic-deleted notification", handler::onTopicDeleted);
        });
        handlers.add("notify-forbidden", c -> {
            var handler = c.get(NotificationHandler.class);
            return bestEffort(UserForbiddenEvent.class,
                "forbidden notification", handler::onUserForbidden);
        });
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
