package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable user experience log.
 */
@Table("bbs_user_exp_log")
public record UserExpLog(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32) String sourceType,
    @Column(length = 64) String sourceId,
    @Column(type = "TEXT") String description,
    @Column int type,            // 0=incr, 1=decr
    @Column int exp,
    @Column long createTime
) {
    public UserExpLog(Long userId, String sourceType, String sourceId, String description, int type, int exp, long createTime) {
        this(null, userId, sourceType, sourceId, description, type, exp, createTime);
    }
}
