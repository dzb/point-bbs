package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_topic_tag")
public record TopicTag(
    @Id @Generated Long id,
    @Column(nullable = false) Long topicId,
    @Column(nullable = false) Long tagId,
    @Column(nullable = false) long status,
    @Column long lastCommentTime,
    @Column Long lastCommentUserId,
    @Column(nullable = false) long createTime
) {
    public TopicTag(Long topicId, Long tagId, long status, long createTime) {
        this(null, topicId, tagId, status, 0L, null, createTime);
    }
}
