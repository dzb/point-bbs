package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_email_code")
public record EmailCode(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(length = 32, nullable = false) String bizType,
    @Column(length = 128, nullable = false) String email,
    @Column(length = 8, nullable = false) String code,
    @Column(length = 32, nullable = false) String token,
    @Column(length = 1024) String title,
    @Column(type = "TEXT") String content,
    @Column(nullable = false) boolean used,
    @Column(nullable = false) long createTime
) {}
