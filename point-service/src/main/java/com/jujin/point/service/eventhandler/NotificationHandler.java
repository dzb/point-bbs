package com.jujin.point.service.eventhandler;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.point.domain.EntityTables;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.event.*;
import com.jujin.point.service.DbQuery;
import com.jujin.point.service.MessageService;
import com.jujin.point.service.Strings;
/**
 * Notification handler — processes domain events and creates user notifications.
 *
 * Dependencies are injected via constructor at container startup.
 * WebSocket push is NOT handled here: MessageService publishes
 * NotificationSentEvent after each persisted row, and the web module pings
 * from that single signal.
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
                        "{\"type\":\"topic\",\"id\":" + e.entityId() + "}"
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
                long parentAuthorId = parentRow.longValue("user_id");
                String parentEntityType = parentRow.string("entity_type");
                long parentEntityId = parentRow.longValue("entity_id");
                String extra =
                    "{\"type\":\"comment\",\"id\":" +
                    e.entityId() +
                    ",\"parentType\":\"" +
                    parentEntityType +
                    "\",\"parentId\":" +
                    parentEntityId +
                    "}";
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
        } else if ("article".equals(e.entityType())) {
            Long articleAuthorId = authorOf(e.entityType(), e.entityId());
            if (articleAuthorId != null) {
                msgSvc.send(
                    e.userId(),
                    articleAuthorId,
                    "新评论",
                    nickname + " 评论了你的文章",
                    null,
                    0,
                    "{\"type\":\"article\",\"id\":" + e.entityId() + "}"
                );
            }
        }
    }

    public void onUserLiked(UserLikedEvent e) {
        var nickname = getNickname(e.userId());
        Long authorId = authorOf(e.entityType(), e.entityId());
        if (authorId != null) {
            msgSvc.send(
                e.userId(),
                authorId,
                "点赞",
                nickname + " 赞了你的" + labelOf(e.entityType()),
                null,
                1,
                "{\"type\":\"" + e.entityType() + "\",\"id\":" + e.entityId() + "}"
            );
        }
    }

    public void onUserFavorited(UserFavoritedEvent e) {
        var nickname = getNickname(e.userId());
        Long authorId = authorOf(e.entityType(), e.entityId());
        if (authorId != null) {
            msgSvc.send(
                e.userId(),
                authorId,
                "新收藏",
                nickname + " 收藏了你的" + labelOf(e.entityType()),
                null,
                4,
                "{\"type\":\"" + e.entityType() + "\",\"id\":" + e.entityId() + "}"
            );
        }
    }

    public void onQaAnswerAccepted(QaAnswerAcceptedEvent e) {
        // e.userId() is the asker; notify the answer's author that their
        // reply was accepted.
        var answerAuthorId = authorOf("comment", e.commentId());
        if (answerAuthorId == null || answerAuthorId == e.userId()) return;
        var nickname = getNickname(e.userId());
        var title = db.query("SELECT title FROM bbs_topic WHERE id = ?", e.topicId())
            .one(Row.class)
            .map(r -> r.string("title"))
            .orElse(null);
        msgSvc.send(
            e.userId(),
            answerAuthorId,
            "回答被采纳",
            nickname + " 采纳了你的回答",
            title != null ? Strings.truncate(title, 200) : null,
            0,
            "{\"type\":\"topic\",\"id\":" + e.topicId() + "}"
        );
    }

    public void onUserMentioned(UserMentionedEvent e) {
        var nickname = getNickname(e.fromUserId());
        msgSvc.send(
            e.fromUserId(),
            e.mentionedUserId(),
            "@了你",
            nickname + " 在" + labelOf(e.entityType()) + "中提到了你",
            e.contentPreview(),
            3,
            "{\"type\":\"" + e.entityType() + "\",\"id\":" + e.entityId() + "}"
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
            "{\"type\":\"user\",\"id\":" + e.userId() + "}"
        );
    }

    /** Admin removed a topic — tell the author. Self-removal notifies nobody. */
    public void onTopicDeleted(TopicDeletedEvent e) {
        if (e.operatorId() == e.authorId()) return;
        var operator = getNickname(e.operatorId());
        var title = db.query("SELECT title FROM bbs_topic WHERE id = ?", e.topicId())
            .one(Row.class)
            .map(r -> r.string("title"))
            .orElse(null);
        msgSvc.send(
            e.operatorId(),
            e.authorId(),
            "帖子被删除",
            operator + " 删除了你的帖子",
            title != null ? Strings.truncate(title, 200) : null,
            0,
            "{\"type\":\"topic\",\"id\":" + e.topicId() + "}"
        );
    }

    /** Mute applied or lifted (forbiddenUntil == 0) — system message. */
    public void onUserForbidden(UserForbiddenEvent e) {
        boolean muted = e.forbiddenUntil() > System.currentTimeMillis();
        msgSvc.send(
            0,
            e.userId(),
            muted ? "禁言通知" : "禁言解除",
            muted
                ? "你的账号已被禁言至 " + Strings.formatTime(e.forbiddenUntil())
                : "你的账号禁言已解除，感谢配合",
            null,
            0,
            null
        );
    }

    /** Author (user_id) of an entity, or null when unknown type / missing row. */
    private Long authorOf(String entityType, long entityId) {
        String table = EntityTables.tableOf(entityType);
        if (table == null) return null;
        return DbQuery.longValue(db,
            "SELECT user_id FROM " + table + " WHERE id = ?", "user_id", entityId);
    }

    private static String labelOf(String entityType) {
        return switch (entityType == null ? "" : entityType) {
            case "topic" -> "帖子";
            case "article" -> "文章";
            case "comment" -> "评论";
            default -> "内容";
        };
    }

    private String getNickname(long userId) {
        return db
            .query("SELECT nickname FROM bbs_user WHERE id = $id")
            .param("id", userId)
            .one(Row.class)
            .map(r -> r.string("nickname"))
            .orElse("有人");
    }
}
