package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_user_task_event")
public record UserTaskEvent(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) int periodKey,
    @Column(nullable = false) Long taskId,
    @Column(nullable = false) int eventCount,
    @Column(nullable = false) int taskFinishCount,
    @Column(nullable = false) long createTime,
    @Column(nullable = false) long updateTime
) {}
