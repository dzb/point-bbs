package com.jujin.point.service;

import com.jujin.point.db.repository.*;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.eventhandler.NotificationHandler;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.EventSubscriber;
import com.jujin.freeway.ioc.Module;
import com.jujin.freeway.ioc.Scope;

/**
 * Service module — binds all business services and event subscribers.
 */
public class ServiceModule implements Module {

    @Override
    public void bind(Binder binder) {
        // Repositories
        binder.bind(UserRepository.class).to(UserRepository.class).scope(Scope.SINGLETON);
        binder.bind(TopicRepository.class).to(TopicRepository.class).scope(Scope.SINGLETON);
        binder.bind(CommentRepository.class).to(CommentRepository.class).scope(Scope.SINGLETON);

        // Core services
        binder.bind(SysConfigService.class).to(SysConfigService.class).scope(Scope.SINGLETON);
        binder.bind(UserService.class).to(UserService.class).scope(Scope.SINGLETON);
        binder.bind(TopicService.class).to(TopicService.class).scope(Scope.SINGLETON);
        binder.bind(CommentService.class).to(CommentService.class).scope(Scope.SINGLETON);
        binder.bind(ArticleService.class).to(ArticleService.class).scope(Scope.SINGLETON);
        binder.bind(CategoryService.class).to(CategoryService.class).scope(Scope.SINGLETON);
        binder.bind(TagService.class).to(TagService.class).scope(Scope.SINGLETON);

        // Interaction services
        binder.bind(UserLikeService.class).to(UserLikeService.class).scope(Scope.SINGLETON);
        binder.bind(FavoriteService.class).to(FavoriteService.class).scope(Scope.SINGLETON);
        binder.bind(UserFollowService.class).to(UserFollowService.class).scope(Scope.SINGLETON);
        binder.bind(MessageService.class).to(MessageService.class).scope(Scope.SINGLETON);

        // Permission & RBAC
        binder.bind(PermissionService.class).to(PermissionService.class).scope(Scope.SINGLETON);

        // Upload
        binder.bind(UploadService.class).to(UploadService.class).scope(Scope.SINGLETON);

        // OAuth
        binder.bind(GitHubOAuthProvider.class).to(GitHubOAuthProvider.class).scope(Scope.SINGLETON);

        // Auth
        binder.bind(AuthService.class).to(AuthService.class).scope(Scope.SINGLETON);

        // --- Event subscribers for notifications ---
        binder.contribute(EventSubscriber.class)
            .add("notify-comment", EventSubscriber.of(CommentCreatedEvent.class,
                e -> NotificationHandler.onCommentCreated(e,
                    AppContextShim.container())));

        binder.contribute(EventSubscriber.class)
            .add("notify-like", EventSubscriber.of(UserLikedEvent.class,
                e -> NotificationHandler.onUserLiked(e,
                    AppContextShim.container())));

        binder.contribute(EventSubscriber.class)
            .add("notify-follow", EventSubscriber.of(UserFollowedEvent.class,
                e -> NotificationHandler.onUserFollowed(e,
                    AppContextShim.container())));
    }

    /**
     * Temporary shim — EventSubscribers are registered at bind() time
     * but need Container access at event time. AppContext provides that.
     */
    static final class AppContextShim {
        static com.jujin.freeway.ioc.Container container() {
            return com.jujin.point.domain.AppContext.container();
        }
    }
}
