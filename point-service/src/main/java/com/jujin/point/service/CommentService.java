package com.jujin.point.service;

import com.jujin.point.db.repository.CommentRepository;
import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.PageResult;
import com.jujin.point.domain.entity.Comment;
import com.jujin.point.domain.event.CommentCreatedEvent;
import com.jujin.point.domain.event.UserMentionedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.ioc.EventBus;

import java.util.HashSet;
import java.util.List;

/**
 * Comment service — create, list, delete.
 */
public class CommentService {
    private final Database db;
    private final CommentRepository commentRepo;
    private final UserRepository userRepo;
    private final EventBus eventBus;

    public CommentService(Database db, CommentRepository commentRepo, UserRepository userRepo, EventBus eventBus) {
        this.db = db;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
        this.eventBus = eventBus;
    }

    public Comment create(long userId, String entityType, long entityId,
                          String content, String contentType, String imageList, long quoteId) {
        if (!"topic".equals(entityType) && !"article".equals(entityType)) {
            throw new ServiceException("未知评论类型: " + entityType);
        }
        var now = System.currentTimeMillis();
        var comment = new Comment();
        comment.setUserId(userId);
        comment.setEntityType(entityType);
        comment.setEntityId(entityId);
        comment.setContent(content);
        comment.setContentType(contentType != null ? contentType : "markdown");
        comment.setImageList(imageList);
        comment.setQuoteId(quoteId);
        comment.setStatus(1);
        comment.setCreateTime(now);

        db.transaction(() -> {
            commentRepo.insert(comment);
            incrCommentCount(entityType, entityId, 1);
            db.execute(
                "UPDATE bbs_user SET comment_count = comment_count + 1 WHERE id = ?",
                userId
            );
            // Bump the parent's sort time so the topic/article resurfaces on
            // lists ordered by last_comment_time.
            touchLastComment(entityType, entityId, userId, now);
            eventBus.publish(new CommentCreatedEvent(userId, comment.getId(), entityId, entityType, now));
            // Reply semantics: a comment with quoteId > 0 also counts against
            // the quoted comment and notifies its author.
            if (quoteId > 0) {
                incrCommentCount("comment", quoteId, 1);
                eventBus.publish(new CommentCreatedEvent(userId, comment.getId(), quoteId, "comment", now));
            }
            // Notify @mentioned users
            var mentioned = MentionParser.extractMentions(content);
            var notified = new HashSet<Long>();
            for (String username : mentioned) {
                userRepo.findByUsername(username).ifPresent(u -> {
                    if (u.getId() != userId && notified.add(u.getId())) {
                        eventBus.publish(new UserMentionedEvent(userId, u.getId(),
                            entityType, entityId, Strings.truncate(content, 100), now));
                    }
                });
            }
        });

        return comment;
    }

    /** Touch the parent entity's last_comment_time / last_comment_user_id. */
    private void touchLastComment(String entityType, long entityId, long userId, long now) {
        String table = switch (entityType) {
            case "topic" -> "bbs_topic";
            case "article" -> "bbs_article";
            default -> null;
        };
        if (table != null) {
            db.execute(
                "UPDATE " + table + " SET last_comment_time = ?, last_comment_user_id = ? WHERE id = ?",
                now, userId, entityId
            );
        }
    }

    private void incrCommentCount(String entityType, long entityId, int delta) {
        String table = switch (entityType) {
            case "topic" -> "bbs_topic";
            case "article" -> "bbs_article";
            case "comment" -> "bbs_comment";
            default -> null;
        };
        if (table != null) {
            // GREATEST guards the decrement path against drifting to negative counts
            db.execute(
                "UPDATE " + table + " SET comment_count = GREATEST(0, comment_count + ?) WHERE id = ?",
                delta, entityId
            );
        }
    }

    public void delete(long userId, long commentId) {
        var comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new ServiceException("评论不存在"));
        if (comment.getUserId() != userId) {
            throw new ServiceException("无权删除");
        }
        comment.setStatus(0); // soft delete
        db.transaction(() -> {
            commentRepo.update(comment);
            // Keep the parent's comment_count consistent with the soft delete
            incrCommentCount(comment.getEntityType(), comment.getEntityId(), -1);
            db.execute(
                "UPDATE bbs_user SET comment_count = GREATEST(0, comment_count - 1) WHERE id = ?",
                comment.getUserId()
            );
            // Re-anchor the parent's sort time on the newest surviving comment
            String table = switch (comment.getEntityType()) {
                case "topic" -> "bbs_topic";
                case "article" -> "bbs_article";
                default -> null;
            };
            if (table != null) {
                db.execute(
                    "UPDATE " + table +
                        " SET last_comment_time = COALESCE((SELECT MAX(create_time) FROM bbs_comment " +
                        "WHERE entity_type = ? AND entity_id = ? AND status = 1), last_comment_time) " +
                        "WHERE id = ?",
                    comment.getEntityType(), comment.getEntityId(), comment.getEntityId()
                );
            }
        });
    }

    public List<Comment> getComments(String entityType, long entityId, PageRequest page) {
        return commentRepo.findByEntity(entityType, entityId, page.page(), page.pageSize());
    }

    public long countComments(String entityType, long entityId) {
        return commentRepo.countByEntity(entityType, entityId);
    }


    public PageResult<Comment> getUserComments(long userId, PageRequest page) {
        var items = commentRepo.findByUserId(userId, page.page(), page.pageSize());
        var total = commentRepo.countByUserId(userId);
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }
}
