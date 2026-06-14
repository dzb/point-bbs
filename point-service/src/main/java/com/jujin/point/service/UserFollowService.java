package com.jujin.point.service;

import com.jujin.point.domain.entity.User;
import com.jujin.point.domain.entity.UserFollow;
import com.jujin.point.domain.event.UserFollowedEvent;
import com.jujin.point.domain.event.UserUnfollowedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.ioc.EventBus;

import java.util.List;

public class UserFollowService {
    private final Database db;
    private final Orm orm;
    private final EventBus bus;

    public UserFollowService(Database db, Orm orm, EventBus bus) {
        this.db = db;
        this.orm = orm;
        this.bus = bus;
    }

    public boolean isFollowing(long userId, long otherId) {
        return DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_follow WHERE user_id = ? AND other_id = ? AND status = 1",
            userId, otherId) > 0;
    }

    public void follow(long userId, long otherId) {
        if (userId == otherId) throw new ServiceException("不能关注自己");
        if (isFollowing(userId, otherId)) return;
        db.transaction(() -> {
            orm.insert(new UserFollow(userId, otherId, 1, System.currentTimeMillis()));
            db.execute("UPDATE bbs_user SET follow_count = follow_count + 1 WHERE id = ?", userId);
            db.execute("UPDATE bbs_user SET fans_count = fans_count + 1 WHERE id = ?", otherId);
            bus.publish(new UserFollowedEvent(userId, otherId, System.currentTimeMillis()));
        });
    }

    public void unfollow(long userId, long otherId) {
        long deleted = db.execute(
            "DELETE FROM bbs_user_follow WHERE user_id = ? AND other_id = ?", userId, otherId).rows();
        if (deleted > 0) {
            db.execute("UPDATE bbs_user SET follow_count = GREATEST(0, follow_count - 1) WHERE id = ?", userId);
            db.execute("UPDATE bbs_user SET fans_count = GREATEST(0, fans_count - 1) WHERE id = ?", otherId);
            bus.publish(new UserUnfollowedEvent(userId, otherId, System.currentTimeMillis()));
        }
    }

    public List<User> getFollowers(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT u.* FROM bbs_user u INNER JOIN bbs_user_follow f ON u.id = f.user_id " +
            "WHERE f.other_id = ? AND f.status = 1 ORDER BY f.create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(User.class);
    }

    public List<User> getFollowing(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT u.* FROM bbs_user u INNER JOIN bbs_user_follow f ON u.id = f.other_id " +
            "WHERE f.user_id = ? AND f.status = 1 ORDER BY f.create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset).list(User.class);
    }
}
