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





    public List<User> findPage(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query("SELECT * FROM bbs_user WHERE status <> 0 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset).list(User.class);
    }

    public long countAll() {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_user WHERE status <> 0")
            .one(Row.class).orElse(null);
        return row != null ? row.longValue("cnt") : 0;
    }


    public List<User> searchByPrefix(String prefix, int limit) {
        // Escape LIKE wildcards with ESCAPE '!' (same dialect-neutral convention
        // as TopicRepository) so %/_ in the prefix match literally.
        var escaped = prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return query(
            "SELECT id, username, nickname, avatar FROM bbs_user WHERE (username LIKE ? ESCAPE '!' OR nickname LIKE ? ESCAPE '!') AND status <> 0 ORDER BY score DESC LIMIT ?",
            escaped + "%", escaped + "%", limit)
            .list(User.class);
    }
}
