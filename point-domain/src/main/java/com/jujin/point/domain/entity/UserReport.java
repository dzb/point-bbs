package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_user_report")
public record UserReport(
    @Id @Generated Long id,
    @Column(nullable = false) Long dataId,
    @Column(length = 32) String dataType,
    @Column(nullable = false) Long userId,
    @Column(type = "TEXT") String reason,
    @Column(length = 16) String auditStatus,
    @Column long auditTime,
    @Column Long auditUserId,
    @Column(nullable = false) long createTime
) {
    public UserReport(Long dataId, String dataType, Long userId, String reason, long createTime) {
        this(null, dataId, dataType, userId, reason, "pending", 0L, null, createTime);
    }
}
