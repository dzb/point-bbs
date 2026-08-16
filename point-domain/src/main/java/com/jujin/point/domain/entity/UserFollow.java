package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Index;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable user-follow record.
 */
@Table("bbs_user_follow")
public record UserFollow(
    @Id @Generated Long id,
    @Index(name = "uq_user_follow", unique = true) @Column(nullable = false) Long userId,
    @Index(name = "uq_user_follow", unique = true) @Column(nullable = false) Long otherId,
    @Column(nullable = false) int status,
    @Column(nullable = false) long createTime
) {
    public UserFollow(Long userId, Long otherId, int status, long createTime) {
        this(null, userId, otherId, status, createTime);
    }
}
