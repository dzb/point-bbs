package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_sms_code")
public record SmsCode(
    @Id @Generated Long id,
    @Column(length = 16, nullable = false) String phone,
    @Column(length = 8, nullable = false) String code,
    @Column(nullable = false) boolean used,
    @Column(nullable = false) long expiredAt,
    @Column(nullable = false) long createTime
) {
    public SmsCode(String phone, String code, long expiredAt, long createTime) {
        this(null, phone, code, false, expiredAt, createTime);
    }
}
