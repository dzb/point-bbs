package com.jujin.point.service;

import com.jujin.point.domain.entity.Favorite;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;

import java.util.List;

public class FavoriteService {
    private final Database db;
    private final Orm orm;

    public FavoriteService(Database db, Orm orm) {
        this.db = db;
        this.orm = orm;
    }

    public boolean isFavorited(long userId, String entityType, long entityId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_favorite WHERE user_id = ? AND entity_type = ? AND entity_id = ?",
            userId, entityType, entityId) > 0;
    }

    public void add(long userId, String entityType, long entityId) {
        if (isFavorited(userId, entityType, entityId)) return;
        orm.insert(new Favorite(userId, entityType, entityId, System.currentTimeMillis()));
    }

    public void remove(long userId, String entityType, long entityId) {
        db.execute("DELETE FROM bbs_favorite WHERE user_id = ? AND entity_type = ? AND entity_id = ?",
            userId, entityType, entityId);
    }

    public List<Favorite> getUserFavorites(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_favorite WHERE user_id = ? ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(Favorite.class);
    }

    public long count(long userId) {
        return DbQuery.count(db, "SELECT COUNT(*) AS cnt FROM bbs_favorite WHERE user_id = ?", userId);
    }
}
