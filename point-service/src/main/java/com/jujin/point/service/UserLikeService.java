package com.jujin.point.service;

import com.jujin.point.domain.entity.UserLike;
import com.jujin.point.domain.event.UserLikedEvent;
import com.jujin.point.domain.event.UserUnlikedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.ioc.EventBus;

import java.util.List;

/**
 * Like/unlike service for topics, articles, and comments.
 */
public class UserLikeService {
    private final Database db;
    private final Orm orm;
    private final EventBus bus;

    public UserLikeService(Database db, EventBus bus) {
        this.db = db;
        this.orm = Orm.of(db);
        this.bus = bus;
    }

    public boolean hasLiked(long userId, String entityType, long entityId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_like WHERE user_id = ? AND entity_type = ? AND entity_id = ?",
            userId, entityType, entityId) > 0;
    }

    /** Like — idempotent, returns true if newly liked. */
    public boolean like(long userId, String entityType, long entityId) {
        if (hasLiked(userId, entityType, entityId)) return false;
        db.transaction(() -> {
            orm.insert(new UserLike(userId, entityId, entityType, System.currentTimeMillis()));
            incrLikeCount(entityType, entityId, 1);
            bus.publish(new UserLikedEvent(userId, entityId, entityType, System.currentTimeMillis()));
        });
        return true;
    }

    /** Unlike — returns true if previously liked. */
    public boolean unlike(long userId, String entityType, long entityId) {
        long deleted = db.execute(
            "DELETE FROM bbs_user_like WHERE user_id = ? AND entity_type = ? AND entity_id = ?",
            userId, entityType, entityId).rows();
        if (deleted > 0) {
            incrLikeCount(entityType, entityId, -1);
            bus.publish(new UserUnlikedEvent(userId, entityId, entityType, System.currentTimeMillis()));
            return true;
        }
        return false;
    }

    public long count(String entityType, long entityId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_like WHERE entity_type = ? AND entity_id = ?",
            entityType, entityId);
    }

    public List<Long> getLikedEntityIds(long userId, String entityType, List<Long> entityIds) {
        if (entityIds.isEmpty()) return List.of();
        var inClause = entityIds.stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("0");
        var rows = db.query(
            "SELECT entity_id FROM bbs_user_like WHERE user_id = ? AND entity_type = ? AND entity_id IN (" + inClause + ")",
            userId, entityType).list(Row.class);
        return rows.stream().map(r -> r.integer("entity_id").longValue()).toList();
    }

    public List<UserLike> getUserLikes(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_user_like WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(UserLike.class);
    }

    private void incrLikeCount(String entityType, long entityId, int delta) {
        String table = switch (entityType) {
            case "topic" -> "bbs_topic";
            case "article" -> "bbs_article";
            case "comment" -> "bbs_comment";
            default -> throw new ServiceException("未知实体类型: " + entityType);
        };
        db.execute("UPDATE " + table + " SET like_count = like_count + ? WHERE id = ?", delta, entityId);
    }
}
