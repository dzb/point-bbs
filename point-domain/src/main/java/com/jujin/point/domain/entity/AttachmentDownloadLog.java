package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_attachment_download_log")
public record AttachmentDownloadLog(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) String attachmentId,
    @Column(nullable = false) long createTime
) {
    public AttachmentDownloadLog(Long userId, String attachmentId, long createTime) {
        this(null, userId, attachmentId, createTime);
    }
}
