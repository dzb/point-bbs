package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_user_role")
public record UserRole(
    @Id @Generated Long id,
    @Column(nullable = false) Long userId,
    @Column(nullable = false) Long roleId,
    @Column(nullable = false) long createTime
) {
    public UserRole(Long userId, Long roleId, long createTime) {
        this(null, userId, roleId, createTime);
    }
}
