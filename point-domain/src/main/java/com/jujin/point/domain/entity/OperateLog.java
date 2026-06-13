package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_operate_log")
public record OperateLog(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32, nullable = false) String opType,
    @Column(length = 32, nullable = false) String dataType,
    @Column(nullable = false) Long dataId,
    @Column(length = 1024, nullable = false) String description,
    @Column(length = 128) String ip,
    @Column(type = "TEXT") String userAgent,
    @Column(type = "TEXT") String referer,
    @Column long createTime
) {
    public OperateLog(Long userId, String opType, String dataType, Long dataId, String description, String ip, String userAgent, String referer, long createTime) {
        this(null, userId, opType, dataType, dataId, description, ip, userAgent, referer, createTime);
    }
}
