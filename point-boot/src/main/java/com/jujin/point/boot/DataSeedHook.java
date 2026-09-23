package com.jujin.point.boot;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.DbConfigKeys;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.RuntimeHook;
import com.jujin.point.domain.auth.Passwords;
import com.jujin.point.service.DbQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Seeds default roles & permissions and the config-gated bootstrap admin
 * (idempotent). Contributed by {@link PointModule} as the
 * {@code data-seed} RuntimeHook.
 */
final class DataSeedHook implements RuntimeHook {
    private static final Logger log = LoggerFactory.getLogger(DataSeedHook.class);

    @Override
    public void start(Container container) {
        var db = container.get(Database.class);
        var symbols = container.get(
            com.jujin.freeway.ioc.symbol.SymbolSource.class
        );

        // ── H2 PostgreSQL-mode index note ──
        // With MODE=PostgreSQL the URL derives PostgresDialect and
        // Schema.ensure creates every entity-declared @Index (and
        // unique index) natively via INFORMATION_SCHEMA.INDEXES.
        // The one index that cannot be entity-declared is
        // idx_follow_other (@Index is not repeatable — other_id
        // already belongs to the unique uq_user_follow), so it is
        // created manually here on both H2 and MySQL.
        var dbUrl = symbols.resolve(DbConfigKeys.URL, null);
        if (dbUrl != null && dbUrl.startsWith("jdbc:")) {
            boolean isH2 = dbUrl.startsWith("jdbc:h2");
            String ddl = isH2
                ? "CREATE INDEX IF NOT EXISTS idx_follow_other ON bbs_user_follow(other_id)"
                : "CREATE INDEX idx_follow_other ON bbs_user_follow(other_id)";
            try {
                db.execute(ddl);
            } catch (Exception e) {
                // MySQL: duplicate-name error means it already exists
                log.warn("index create skipped: {}", e.getMessage());
            }
        }

        // ── Bootstrap admin (config-gated) ──
        // When bbs.admin.username + bbs.admin.password are set
        // and no user holds the admin permission yet, create
        // the admin account and assign the admin role. Default
        // configs leave both empty — no-op.
        String adminName = symbols.resolve("bbs.admin.username", null);
        String adminPass = symbols.resolve("bbs.admin.password", null);
        boolean hasAdmin = DbQuery.count(
            db,
            "SELECT COUNT(*) AS cnt FROM bbs_user_role ur JOIN bbs_role_permission rp ON ur.role_id = rp.role_id " +
            "JOIN bbs_permission p ON rp.permission_id = p.id WHERE p.code = 'admin' AND p.status = 1"
        ) > 0;
        long adminUserExists = DbQuery.count(
            db,
            "SELECT COUNT(*) AS cnt FROM bbs_user WHERE username = ?",
            adminName
        );
        if (
            !hasAdmin &&
            adminName != null &&
            !adminName.isBlank() &&
            adminPass != null &&
            adminPass.length() >= 8 &&
            adminUserExists == 0
        ) {
            long t = System.currentTimeMillis();
            String pw = Passwords.hash(adminPass);
            db.execute(
                "INSERT INTO bbs_user (create_time,update_time,nickname,username,password,email_verified,score,exp,level,status,topic_count,comment_count,follow_count,fans_count,forbidden_end_time) VALUES (?,?,?,?,?,true,0,0,1,1,0,0,0,0,0)",
                t, t, adminName, adminName, pw
            );
            var inserted = db
                .query(
                    "SELECT id FROM bbs_user WHERE username = ?",
                    adminName
                )
                .one(Row.class);
            inserted.ifPresent(row ->
                db.execute(
                    "INSERT INTO bbs_user_role (user_id, role_id, create_time) VALUES (?,1,?)",
                    row.longValue("id"),
                    t
                )
            );
            log.info(
                "[seed] Bootstrap admin '{}' created",
                adminName
            );
        }

        long count = DbQuery.count(
            db,
            "SELECT COUNT(*) AS cnt FROM bbs_permission"
        );
        if (count > 0) return;

        long now = System.currentTimeMillis();
        String[][] perms = {
            { "admin", "admin", "管理员权限", "系统" },
            { "admin", "topic:manage", "帖子管理", "内容" },
            { "admin", "user:manage", "用户管理", "用户" },
            { "admin", "config:manage", "配置管理", "系统" },
            { "admin", "article:manage", "文章管理", "内容" },
        };
        for (String[] p : perms) {
            db.execute(
                "INSERT INTO bbs_permission (type, code, name, group_name, sort_no, status, create_time, update_time) VALUES (?,?,?,?,0,1,?,?)",
                (Object) p[0],
                (Object) p[1],
                (Object) p[2],
                (Object) p[3],
                now,
                now
            );
        }
        db.execute(
            "INSERT INTO bbs_role (type, name, code, sort_no, status, create_time, update_time) VALUES (0,'管理员','admin',0,1,?,?)",
            now,
            now
        );
        var rows = db
            .query("SELECT id FROM bbs_permission")
            .list(Row.class);
        for (var r : rows) {
            db.execute(
                "INSERT INTO bbs_role_permission (role_id, permission_id, create_time) VALUES (1,?,?)",
                r.longValue("id"),
                now
            );
        }
        log.info("[seed] Default roles & permissions created");
    }

    @Override
    public void stop(Container container) {}
}
