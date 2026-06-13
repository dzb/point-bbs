package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Check-in entity — immutable per-user check-in record.
 */
@Table("bbs_check_in")
public record CheckIn(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32) String latestDayName,
    @Column(nullable = false) int consecutiveDays,
    @Column(nullable = false) long createTime,
    @Column(nullable = false) long updateTime
) {
    public CheckIn(Long userId, String latestDayName, int consecutiveDays, long createTime, long updateTime) {
        this(null, userId, latestDayName, consecutiveDays, createTime, updateTime);
    }
}
