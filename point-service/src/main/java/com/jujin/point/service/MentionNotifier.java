package com.jujin.point.service;

import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.event.UserMentionedEvent;
import com.jujin.freeway.ioc.EventBus;

import java.util.HashSet;

/**
 * Resolves @mentions in content and publishes one UserMentionedEvent per
 * distinct mentioned user (never the author, never duplicated).
 *
 * Previously copy-pasted across TopicService, CommentService and
 * ArticleService — the article copy even used raw SQL for the user lookup.
 */
public class MentionNotifier {
    private final UserRepository userRepo;
    private final EventBus eventBus;

    public MentionNotifier(UserRepository userRepo, EventBus eventBus) {
        this.userRepo = userRepo;
        this.eventBus = eventBus;
    }

    /** Extract mentions from {@code content} and publish mention events. */
    public void notifyMentions(long actorId, String content, String entityType,
                               long entityId, long timestamp) {
        var notified = new HashSet<Long>();
        for (String username : MentionParser.extractMentions(content)) {
            userRepo.findByUsername(username).ifPresent(u -> {
                if (u.getId() != actorId && notified.add(u.getId())) {
                    eventBus.publish(new UserMentionedEvent(
                        actorId, u.getId(), entityType, entityId,
                        Strings.truncate(content, 100), timestamp));
                }
            });
        }
    }
}
