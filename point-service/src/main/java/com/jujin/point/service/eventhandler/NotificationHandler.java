package com.jujin.point.service.eventhandler;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.DbQuery;
import com.jujin.point.service.MessageService;
import com.jujin.point.service.Strings;

/**
 * Notification handler — processes domain events and creates user notifications.
 *
 * Freeway 1.3.2: dependencies are injected via constructor at container startup,
 * not resolved from Container at runtime. This is the idiomatic DI pattern.
 */
public class NotificationHandler {

    private final Database db;
    private final MessageService msgSvc;

    public NotificationHandler(Database db, MessageService msgSvc) {
        this.db = db;
        this.msgSvc = msgSvc;
    }

    public void onCommentCreated(CommentCreatedEvent e) {
        var nickname = getNickname(e.userId());
        if ("topic".equals(e.entityType())) {
            db.query("SELECT user_id, title FROM bbs_topic WHERE id = $id")
                .param("id", e.entityId())
                .one(Topic.class)
                .ifPresent(topic ->
                    msgSvc.send(
                        e.userId(),
                        topic.getUserId(),
                        "新评论",
                        nickname + " 评论了你的帖子",
                        Strings.truncate(topic.getTitle(), 200),
                        0,
                        "topic:" + e.entityId()
                    )
                );
        } else if ("comment".equals(e.entityType())) {
            var parentRow = db
                .query(
                    "SELECT user_id, entity_type, entity_id FROM bbs_comment WHERE id = ?",
                    e.entityId()
                )
                .one(Row.class)
                .orElse(null);
            if (parentRow != null) {
                long parentAuthorId = parentRow.longVal("user_id");
                String parentEntityType = parentRow.string("entity_type");
                long parentEntityId = parentRow.longVal("entity_id");
                if (parentAuthorId != e.userId()) {
                    String extra =
                        "comment:" +
                        e.entityId() +
                        ":" +
                        parentEntityType +
                        ":" +
                        parentEntityId;
                    msgSvc.send(
                        e.userId(),
                        parentAuthorId,
                        "新回复",
                        nickname + " 回复了你的评论",
                        null,
                        0,
                        extra
                    );
                }
            }
        } else if ("article".equals(e.entityType())) {
            Long articleAuthorId = DbQuery.longValue(
                db,
                "SELECT user_id FROM bbs_article WHERE id = ?",
                "user_id",
                e.entityId()
            );
            if (articleAuthorId != null && articleAuthorId != e.userId()) {
                msgSvc.send(
                    e.userId(),
                    articleAuthorId,
                    "新评论",
                    nickname + " 评论了你的文章",
                    null,
                    0,
                    "article:" + e.entityId()
                );
            }
        }
    }

    public void onUserLiked(UserLikedEvent e) {
        var nickname = getNickname(e.userId());
        Long authorId = switch (e.entityType()) {
            case "topic" -> DbQuery.longValue(
                db,
                "SELECT user_id FROM bbs_topic WHERE id = ?",
                "user_id",
                e.entityId()
            );
            case "comment" -> DbQuery.longValue(
                db,
                "SELECT user_id FROM bbs_comment WHERE id = ?",
                "user_id",
                e.entityId()
            );
            default -> null;
        };
        if (authorId != null) {
            msgSvc.send(
                e.userId(),
                authorId,
                "点赞",
                nickname + " 赞了你的" + e.entityType(),
                null,
                1,
                e.entityType() + ":" + e.entityId()
            );
        }
    }

    public void onUserMentioned(UserMentionedEvent e) {
        var nickname = getNickname(e.fromUserId());
        var entityLabel = switch (e.entityType()) {
            case "topic" -> "帖子";
            case "article" -> "文章";
            case "comment" -> "评论";
            default -> "内容";
        };
        msgSvc.send(
            e.fromUserId(),
            e.mentionedUserId(),
            "@了你",
            nickname + " 在" + entityLabel + "中提到了你",
            e.contentPreview(),
            3,
            e.entityType() + ":" + e.entityId()
        );
    }

    public void onUserFollowed(UserFollowedEvent e) {
        var nickname = getNickname(e.userId());
        msgSvc.send(
            e.userId(),
            e.otherId(),
            "新粉丝",
            nickname + " 关注了你",
            null,
            2,
            "user:" + e.userId()
        );
    }

    private String getNickname(long userId) {
        return db
            .query("SELECT nickname FROM bbs_user WHERE id = $id")
            .param("id", userId)
            .list(Row.class)
            .stream()
            .findFirst()
            .map(r -> r.string("nickname"))
            .orElse("有人");
    }
}
