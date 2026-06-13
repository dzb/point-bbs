package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_user_feed")
public record UserFeed(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) Long dataId,
    @Column(length = 64) String dataType,
    @Column Long authorId,
    @Column(nullable = false) long createTime
) {
    public UserFeed(Long userId, Long dataId, String dataType, Long authorId, long createTime) {
        this(null, userId, dataId, dataType, authorId, createTime);
    }
}
