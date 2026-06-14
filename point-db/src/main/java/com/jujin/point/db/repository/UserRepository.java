package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.User;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;

import java.util.List;
import java.util.Optional;

/**
 * User repository providing common queries beyond base CRUD.
 */
public class UserRepository extends BaseRepository<User> {

    public UserRepository(Database db, Orm orm) {
        super(db, orm, User.class);
    }

    public Optional<User> findByEmail(String email) {
        return query("SELECT * FROM bbs_user WHERE email = $email AND status <> 0")
            .param("email", email).one(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return query("SELECT * FROM bbs_user WHERE username = $username AND status <> 0")
            .param("username", username).one(User.class);
    }

    public Optional<User> findByPhone(String phone) {
        return query("SELECT * FROM bbs_user WHERE phone = $phone AND status <> 0")
            .param("phone", phone).one(User.class);
    }

    public Optional<User> findByNickname(String nickname) {
        return query("SELECT * FROM bbs_user WHERE nickname = $nickname AND status <> 0")
            .param("nickname", nickname).one(User.class);
    }

    public List<User> findTopByScore(int limit) {
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY score DESC LIMIT $limit")
            .param("limit", limit).list(User.class);
    }

    public List<User> findLatest(int limit) {
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY create_time DESC LIMIT $limit")
            .param("limit", limit).list(User.class);
    }

    public void incrTopicCount(long userId) {
        execute("UPDATE bbs_user SET topic_count = topic_count + 1 WHERE id = ?", userId);
    }

    public List<User> findPage(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset).list(User.class);
    }

    public long countAll() {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_user WHERE status <> 0")
            .one(Row.class).orElse(null);
        return row != null ? row.integer("cnt").longValue() : 0;
    }

    public void incrCommentCount(long userId) {
        execute("UPDATE bbs_user SET comment_count = comment_count + 1 WHERE id = ?", userId);
    }
}
