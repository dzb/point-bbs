package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.Comment;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
            .bind("entityType", entityType).bind("entityId", entityId)
            .bind("limit", pageSize).bind("offset", offset)
            .list(Comment.class);
    }


    /**
     * Live comment contents for a set of ids — used for quote previews.
     * Only {@code id} and {@code content} come back; the empty set
     * short-circuits because {@code IN ()} is invalid on both dialects.
     */
    public List<Comment> findContentsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        var placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
        return query(
            "SELECT id, content FROM bbs_comment WHERE id IN (" + placeholders + ") AND status = 1",
            ids.toArray())
            .list(Comment.class);
    }

    public List<Comment> findByUserId(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_comment WHERE user_id = $userId AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .bind("userId", userId).bind("limit", pageSize).bind("offset", offset)
            .list(Comment.class);
    }

    public long countByEntity(String entityType, long entityId) {
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_comment WHERE entity_type = $entityType AND entity_id = $entityId AND status = 1")
            .bind("entityType", entityType).bind("entityId", entityId)
            .one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    public long countByUserId(long userId) {
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_comment WHERE user_id = $userId AND status = 1")
            .bind("userId", userId)
            .one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }
}
