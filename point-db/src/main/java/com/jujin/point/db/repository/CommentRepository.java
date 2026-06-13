package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.Comment;
import com.jujin.freeway.db.Database;

import java.util.List;

/**
 * Comment repository.
 */
public class CommentRepository extends BaseRepository<Comment> {

    public CommentRepository(Database db) {
        super(db, Comment.class);
    }

    public List<Comment> findByEntity(String entityType, long entityId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_comment WHERE entity_type = ? AND entity_id = ? AND status = 1 " +
            "ORDER BY create_time ASC LIMIT ? OFFSET ?",
            entityType, entityId, pageSize, offset
        ).list(Comment.class);
    }

    public List<Comment> findReplies(long commentId, int page, int pageSize) {
        return findByEntity("comment", commentId, page, pageSize);
    }

    public List<Comment> findByUserId(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_comment WHERE user_id = ? AND status = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset
        ).list(Comment.class);
    }

    public long countByEntity(String entityType, long entityId) {
        var list = query(
            "SELECT * FROM bbs_comment WHERE entity_type = ? AND entity_id = ? AND status = 1",
            entityType, entityId
        ).list(Comment.class);
        return list.size();
    }
}
