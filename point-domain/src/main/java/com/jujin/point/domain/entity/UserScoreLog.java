package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Immutable user score log.
 */
@Table("bbs_user_score_log")
public record UserScoreLog(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32) String sourceType,
    @Column(length = 32) String sourceId,
    @Column(type = "TEXT") String description,
    @Column int type,            // 0=incr, 1=decr
    @Column int score,
    @Column long createTime
) {
    public UserScoreLog(Long userId, String sourceType, String sourceId, String description, int type, int score, long createTime) {
        this(null, userId, sourceType, sourceId, description, type, score, createTime);
    }
}
