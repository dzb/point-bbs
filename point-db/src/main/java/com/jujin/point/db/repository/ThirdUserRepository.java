package com.jujin.point.db.repository;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.point.domain.entity.ThirdUser;

import java.util.Optional;

/**
 * Third-party account bindings ({@code bbs_third_user}).
 *
 * <p>{@code (open_id, third_type)} carries a unique index, so lookups may
 * legitimately race with an insert from a concurrent callback.</p>
 */
public class ThirdUserRepository extends BaseRepository<ThirdUser> {

    public ThirdUserRepository(Database db, Orm orm) {
        super(db, orm, ThirdUser.class);
    }

    public Optional<ThirdUser> findByOpenIdAndType(String openId, String thirdType) {
        return query(
            "SELECT * FROM bbs_third_user WHERE open_id = $openId AND third_type = $thirdType")
            .bind("openId", openId).bind("thirdType", thirdType)
            .one(ThirdUser.class);
    }

    public boolean exists(String openId, String thirdType) {
        return query(
            "SELECT 1 FROM bbs_third_user WHERE open_id = $openId AND third_type = $thirdType")
            .bind("openId", openId).bind("thirdType", thirdType)
            .one(ThirdUser.class).isPresent();
    }

    public int deleteByUserIdAndType(long userId, String thirdType) {
        return execute(
            "DELETE FROM bbs_third_user WHERE user_id = ? AND third_type = ?",
            userId, thirdType);
    }
}
