package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_email_log")
public record EmailLog(
    @Id @Generated Long id,
    @Column(length = 128, nullable = false) String toEmail,
    @Column(length = 1024) String title,
    @Column(type = "TEXT") String content,
    @Column(length = 128) String ip,
    @Column(type = "TEXT") String userAgent,
    @Column(nullable = false) long createTime
) {
    public EmailLog(String toEmail, String title, String content, String ip, String userAgent, long createTime) {
        this(null, toEmail, title, content, ip, userAgent, createTime);
    }
}
