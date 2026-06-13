package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable favorite/bookmark record.
 */
@Table("bbs_favorite")
public record Favorite(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32, nullable = false) String entityType,
    @Column(nullable = false) Long entityId,
    @Column(nullable = false) long createTime
) {
    public Favorite(Long userId, String entityType, Long entityId, long createTime) {
        this(null, userId, entityType, entityId, createTime);
    }
}
