package com.jujin.point.service.eventhandler;

import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.DbQuery;
import com.jujin.point.service.MessageService;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.ioc.Container;

public final class NotificationHandler {
    private NotificationHandler() {}

    public static void onCommentCreated(CommentCreatedEvent e, Container c) {
        var msgSvc = c.get(MessageService.class);
        if ("topic".equals(e.entityType())) {
            var db = c.get(Database.class);
            db.query("SELECT user_id, title FROM bbs_topic WHERE id = ?", e.entityId())
                .one(Topic.class)
                .ifPresent(topic ->
                    msgSvc.send(e.userId(), topic.getUserId(),
                        "新评论", "评论了你的帖子",
                        truncate(topic.getTitle(), 200), 0,
                        e.entityType() + ":" + e.entityId()));
        }
    }

    public static void onUserLiked(UserLikedEvent e, Container c) {
        var db = c.get(Database.class);
        var msgSvc = c.get(MessageService.class);

        Long authorId = switch (e.entityType()) {
            case "topic" -> DbQuery.longValue(db,
                "SELECT user_id FROM bbs_topic WHERE id = ?", "user_id", e.entityId());
            case "comment" -> DbQuery.longValue(db,
                "SELECT user_id FROM bbs_comment WHERE id = ?", "user_id", e.entityId());
            default -> null;
        };
        if (authorId != null) {
            msgSvc.send(e.userId(), authorId,
                "点赞", "赞了你的" + e.entityType(), null, 1,
                e.entityType() + ":" + e.entityId());
        }
    }

    public static void onUserFollowed(UserFollowedEvent e, Container c) {
        c.get(MessageService.class).send(e.userId(), e.otherId(),
            "新粉丝", "关注了你", null, 2, "user:" + e.userId());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
