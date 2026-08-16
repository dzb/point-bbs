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

/**
 * Service module — binds all business services and event subscribers.
 *
 * freeway 1.3.8: SINGLETON is the default scope, so explicit .scope() is omitted.
 * Event subscribers use canonical IDs for ordering and rely on container-injected
 * NotificationHandler (not AppContext.container()).
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

        // Permission & RBAC
        binder.bind(PermissionService.class).to(PermissionService.class);

        // Upload
        binder.bind(UploadService.class).to(UploadService.class);

        // OAuth
        binder.bind(GitHubOAuthProvider.class).to(GitHubOAuthProvider.class);

        // Auth
        binder.bind(AuthService.class).to(AuthService.class);

        // Event subscribers — NotificationHandler receives DI via container
        binder.bind(NotificationHandler.class).to(NotificationHandler.class);

        // Notification handlers are best-effort: a failure in the handler must
        // never roll back the business operation that published the event.
        binder
            .contribute(EventSubscriber.class)
            .add(
                "notify-comment",
                EventSubscriber.of(CommentCreatedEvent.class, e -> {
                    try {
                        AppContext.get(NotificationHandler.class).onCommentCreated(e);
                    } catch (Exception ex) {
                        log.warn("comment notification failed", ex);
                    }
                })
            );

        binder
            .contribute(EventSubscriber.class)
            .add(
                "notify-like",
                EventSubscriber.of(UserLikedEvent.class, e -> {
                    try {
                        AppContext.get(NotificationHandler.class).onUserLiked(e);
                    } catch (Exception ex) {
                        log.warn("notification failed", ex);
                    }
                })
            );

        binder
            .contribute(EventSubscriber.class)
            .add(
                "notify-follow",
                EventSubscriber.of(UserFollowedEvent.class, e -> {
                    try {
                        AppContext.get(NotificationHandler.class).onUserFollowed(e);
                    } catch (Exception ex) {
                        log.warn("notification failed", ex);
                    }
                })
            );

        binder
            .contribute(EventSubscriber.class)
            .add(
                "notify-mention",
                EventSubscriber.of(UserMentionedEvent.class, e -> {
                    try {
                        AppContext.get(NotificationHandler.class).onUserMentioned(e);
                    } catch (Exception ex) {
                        log.warn("notification failed", ex);
                    }
                })
            );
    }
}
