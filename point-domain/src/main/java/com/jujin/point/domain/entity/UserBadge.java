package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_user_badge")
public record UserBadge(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) Long badgeId,
    @Column(length = 32, nullable = false) String sourceType,
    @Column(length = 64, nullable = false) String sourceId,
    @Column(nullable = false) boolean isWorn,
    @Column int sortNo,
    @Column(nullable = false) long createTime,
    @Column(nullable = false) long updateTime
) {}
