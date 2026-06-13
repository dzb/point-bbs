package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.User;
import com.jujin.freeway.db.Database;

import java.util.List;
import java.util.Optional;

/**
 * User repository providing common queries beyond base CRUD.
 */
public class UserRepository extends BaseRepository<User> {

    public UserRepository(Database db) {
        super(db, User.class);
    }

    public Optional<User> findByEmail(String email) {
        return query("SELECT * FROM bbs_user WHERE email = ? AND status <> 0", email)
            .one(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return query("SELECT * FROM bbs_user WHERE username = ? AND status <> 0", username)
            .one(User.class);
    }

    public Optional<User> findByPhone(String phone) {
        return query("SELECT * FROM bbs_user WHERE phone = ? AND status <> 0", phone)
            .one(User.class);
    }

    public Optional<User> findByNickname(String nickname) {
        return query("SELECT * FROM bbs_user WHERE nickname = ? AND status <> 0", nickname)
            .one(User.class);
    }

    public List<User> findTopByScore(int limit) {
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY score DESC LIMIT ?", limit)
            .list(User.class);
    }

    public List<User> findLatest(int limit) {
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY create_time DESC LIMIT ?", limit)
            .list(User.class);
    }

    public void incrTopicCount(long userId) {
        execute("UPDATE bbs_user SET topic_count = topic_count + 1 WHERE id = ?", userId);
    }

    public void incrCommentCount(long userId) {
        execute("UPDATE bbs_user SET comment_count = comment_count + 1 WHERE id = ?", userId);
    }
}
