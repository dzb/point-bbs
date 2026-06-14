package com.jujin.point.service;

import com.jujin.point.domain.entity.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;

import java.util.*;

/**
 * Permission and role-based access control service.
 */
public class PermissionService {
    private final Database db;
    private final Orm orm;

    public PermissionService(Database db, Orm orm) {
        this.db = db;
        this.orm = orm;
    }

    /** Check if a user has a specific permission code. */
    public boolean hasPermission(long userId, String permCode) {
        var rows = db.query("""
            SELECT 1 FROM bbs_user_role ur
            JOIN bbs_role_permission rp ON ur.role_id = rp.role_id
            JOIN bbs_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = ? AND p.code = ? AND p.status = 1
            """, userId, permCode).list(Row.class);
        return !rows.isEmpty();
    }

    /** Get all permission codes for a user. */
    public Set<String> getUserPermissionCodes(long userId) {
        var rows = db.query("""
            SELECT p.code FROM bbs_user_role ur
            JOIN bbs_role_permission rp ON ur.role_id = rp.role_id
            JOIN bbs_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = ? AND p.status = 1
            """, userId).list(Row.class);
        Set<String> codes = new HashSet<>();
        for (var r : rows) {
            String code = r.string("code");
            if (code != null) codes.add(code);
        }
        return codes;
    }

    /** Assign a role to a user. */
    public void assignRole(long userId, long roleId) {
        long count = DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_role WHERE user_id=? AND role_id=?", userId, roleId);
        if (count == 0) {
            orm.insert(new UserRole(userId, roleId, System.currentTimeMillis()));
        }
    }

    /** Remove a role from a user. */
    public void removeRole(long userId, long roleId) {
        db.execute("DELETE FROM bbs_user_role WHERE user_id=? AND role_id=?", userId, roleId);
    }

    // --- Role CRUD ---

    public List<Role> getAllRoles() {
        return orm.findAll(Role.class);
    }

    public Role createRole(String name, String code) {
        var r = new Role();
        var now = System.currentTimeMillis();
        r.setName(name);
        r.setCode(code);
        r.setType(1);
        r.setStatus(1);
        r.setCreateTime(now);
        r.setUpdateTime(now);
        orm.insert(r);
        return r;
    }

    public void assignPermission(long roleId, long permissionId) {
        long count = DbQuery.count(db,
            "SELECT COUNT(*) AS cnt FROM bbs_role_permission WHERE role_id=? AND permission_id=?", roleId, permissionId);
        if (count == 0) {
            orm.insert(new RolePermission(roleId, permissionId, System.currentTimeMillis()));
        }
    }

    // --- Permissions CRUD ---

    public List<Permission> getAllPermissions() {
        return db.query("SELECT * FROM bbs_permission WHERE status = 1 ORDER BY group_name, sort_no")
            .list(Permission.class);
    }

    public Permission createPermission(String type, String code, String name, String groupName) {
        var p = new Permission();
        var now = System.currentTimeMillis();
        p.setType(type);
        p.setCode(code);
        p.setName(name);
        p.setGroupName(groupName);
        p.setStatus(1);
        p.setCreateTime(now);
        p.setUpdateTime(now);
        orm.insert(p);
        return p;
    }
}
