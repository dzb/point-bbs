package com.jujin.point.service;

import com.jujin.point.db.repository.CommentRepository;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.PageResult;
import com.jujin.point.domain.entity.Comment;
import com.jujin.point.domain.event.CommentCreatedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.ioc.EventBus;

import java.util.List;

/**
 * Comment service — create, list, delete.
 */
public class CommentService {
    private final Database db;
    private final CommentRepository commentRepo;
    private final EventBus eventBus;

    public CommentService(Database db, CommentRepository commentRepo, EventBus eventBus) {
        this.db = db;
        this.commentRepo = commentRepo;
        this.eventBus = eventBus;
    }

    public Comment create(long userId, String entityType, long entityId,
                          String content, String contentType, String imageList, long quoteId) {
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
            eventBus.publish(new CommentCreatedEvent(userId, comment.getId(), entityId, entityType, now));
        });

        return comment;
    }

    private void incrCommentCount(String entityType, long entityId, int delta) {
        String table = switch (entityType) {
            case "topic" -> "bbs_topic";
            case "article" -> "bbs_article";
            default -> null;
        };
        if (table != null) {
            db.execute("UPDATE " + table + " SET comment_count = comment_count + ? WHERE id = ?", delta, entityId);
        }
    }

    public void delete(long userId, long commentId) {
        var comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new ServiceException("评论不存在"));
        if (comment.getUserId() != userId) {
            throw new ServiceException("无权删除");
        }
        comment.setStatus(0); // soft delete
        commentRepo.update(comment);
    }

    public List<Comment> getComments(String entityType, long entityId, PageRequest page) {
        return commentRepo.findByEntity(entityType, entityId, page.page(), page.pageSize());
    }

    public List<Comment> getReplies(long commentId, PageRequest page) {
        return commentRepo.findReplies(commentId, page.page(), page.pageSize());
    }

    public PageResult<Comment> getUserComments(long userId, PageRequest page) {
        var items = commentRepo.findByUserId(userId, page.page(), page.pageSize());
        return new PageResult<>(items, page.page(), page.pageSize(), items.size());
    }
}
