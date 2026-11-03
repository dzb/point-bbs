package com.jujin.point.service;

import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.freeway.ioc.RuntimeHook;
import com.jujin.freeway.ioc.event.EventBus;
import com.jujin.point.db.repository.*;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.eventhandler.NotificationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service module — binds all business services and event subscribers.
 *
 * SINGLETON is the default scope, so explicit .scope() is omitted.
 *
 * <p>Subscribers are registered as runtime subscriptions from a
 * {@link RuntimeHook} (start phase), not as composition-time
 * {@code EventSubscriber} contributions. Reason: the bus freezes its
 * subscription table when the container first resolves it, and the
 * contribution factories here resolve {@link NotificationHandler} →
 * {@code MessageService} → {@link EventBus} — a construction cycle that
 * left the bus built with an empty table, so no notification ever fired.
 * Registering after the container is composed breaks the cycle: the bus is
 * already built, {@code subscribe(...)} attaches to the runtime index, and
 * an event-time failure is still isolated by the bus (thrown subscriber
 * failures are logged and do not propagate to the publisher).</p>
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

        binder.contribute(RuntimeHook.class).add("point-notification-subscribers",
            new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var bus = container.get(EventBus.class);
                    var handler = container.get(NotificationHandler.class);
                    bus.subscribe(CommentCreatedEvent.class, handler::onCommentCreated);
                    bus.subscribe(UserLikedEvent.class, handler::onUserLiked);
                    bus.subscribe(UserFavoritedEvent.class, handler::onUserFavorited);
                    bus.subscribe(UserFollowedEvent.class, handler::onUserFollowed);
                    bus.subscribe(UserMentionedEvent.class, handler::onUserMentioned);
                    bus.subscribe(QaAnswerAcceptedEvent.class, handler::onQaAnswerAccepted);
                    bus.subscribe(TopicDeletedEvent.class, handler::onTopicDeleted);
                    bus.subscribe(UserForbiddenEvent.class, handler::onUserForbidden);
                    log.info("notification subscribers registered on the event bus");
                }

                @Override
                public void stop(Container container) {
                    // Runtime subscriptions are cleared by EventBus.close().
                }
            });
    }
}
