package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.Comment;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;

import java.util.List;

/**
 * Comment repository.
 */
public class CommentRepository extends BaseRepository<Comment> {

    public CommentRepository(Database db, Orm orm) {
        super(db, orm, Comment.class);
    }

    public List<Comment> findByEntity(String entityType, long entityId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_comment WHERE entity_type = $entityType AND entity_id = $entityId AND status = 1 " +
            "ORDER BY create_time ASC LIMIT $limit OFFSET $offset")
            .param("entityType", entityType).param("entityId", entityId)
            .param("limit", pageSize).param("offset", offset)
            .list(Comment.class);
    }

    public List<Comment> findReplies(long commentId, int page, int pageSize) {
        return findByEntity("comment", commentId, page, pageSize);
    }

    public List<Comment> findByUserId(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_comment WHERE user_id = $userId AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("userId", userId).param("limit", pageSize).param("offset", offset)
            .list(Comment.class);
    }

    public long countByEntity(String entityType, long entityId) {
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_comment WHERE entity_type = $entityType AND entity_id = $entityId AND status = 1")
            .param("entityType", entityType).param("entityId", entityId)
            .one(Row.class).orElse(null);
        return row != null ? row.get("cnt", long.class) : 0;
    }
}
