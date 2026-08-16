package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Index;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable user-like record (unique per user + entity).
 */
@Table("bbs_user_like")
public record UserLike(
    @Id @Generated Long id,
    @Index(name = "uq_user_like", unique = true) @Column(nullable = false) Long userId,
    @Index(name = "uq_user_like", unique = true) @Column(nullable = false) Long entityId,
    @Index(name = "uq_user_like", unique = true) @Column(length = 32, nullable = false) String entityType,
    @Column(nullable = false) long createTime
) {
    public UserLike(Long userId, Long entityId, String entityType, long createTime) {
        this(null, userId, entityId, entityType, createTime);
    }
}
