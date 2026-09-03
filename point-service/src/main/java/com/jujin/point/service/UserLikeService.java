package com.jujin.point.service;

import com.jujin.point.domain.EntityTables;
import com.jujin.point.domain.entity.UserLike;
import com.jujin.point.domain.event.UserLikedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.ioc.EventBus;

import java.util.List;

/**
 * Like/unlike service for topics, articles, and comments.
 */
public class UserLikeService {
    private final Database db;
    private final Orm orm;
    private final EventBus bus;

    public UserLikeService(Database db, Orm orm, EventBus bus) {
        this.db = db;
        this.orm = orm;
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
        try {
            db.transaction(() -> {
                orm.insert(new UserLike(userId, entityId, entityType, System.currentTimeMillis()));
                incrLikeCount(entityType, entityId, 1);
                bus.publish(new UserLikedEvent(userId, entityId, entityType, System.currentTimeMillis()));
            });
        } catch (com.jujin.freeway.db.SqlException e) {
            // Unique (user_id, entity_id, entity_type) index — a concurrent
            // request already inserted this like; treat as already-liked.
            return false;
        }
        return true;
    }

    /** Unlike — returns true if previously liked. */
    public boolean unlike(long userId, String entityType, long entityId) {
        var removed = new boolean[1];
        db.transaction(() -> {
            long deleted = db.execute(
                "DELETE FROM bbs_user_like WHERE user_id = ? AND entity_type = ? AND entity_id = ?",
                userId, entityType, entityId).rows();
            if (deleted > 0) {
                removed[0] = true;
                incrLikeCount(entityType, entityId, -1);
            }
        });
        return removed[0];
    }

    public long count(String entityType, long entityId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_like WHERE entity_type = ? AND entity_id = ?",
            entityType, entityId);
    }


    public List<UserLike> getUserLikes(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_user_like WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(UserLike.class);
    }

    private void incrLikeCount(String entityType, long entityId, int delta) {
        String table = EntityTables.tableOf(entityType);
        if (table == null) {
            throw new ServiceException("未知实体类型: " + entityType);
        }
        db.execute(
            "UPDATE " + table + " SET like_count = GREATEST(0, like_count + ?) WHERE id = ?",
            delta, entityId
        );
    }
}
