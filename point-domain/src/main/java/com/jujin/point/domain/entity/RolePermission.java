package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_role_permission")
public record RolePermission(
    @Id @Generated Long id,
    @Column(nullable = false) Long roleId,
    @Column(nullable = false) Long permissionId,
    @Column(nullable = false) long createTime
) {
    public RolePermission(Long roleId, Long permissionId, long createTime) {
        this(null, roleId, permissionId, createTime);
    }
}
