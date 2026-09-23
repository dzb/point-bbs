package com.jujin.point.boot;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.DbModule;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.db.schema.SchemaEntity;
import com.jujin.freeway.http.filter.HealthCheck;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.freeway.ioc.RuntimeHook;
import com.jujin.point.domain.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

/**
 * Primary bbs module — contributes schema entities, health check and the seed
 * hooks ({@link DataSeedHook}, {@link DevDataSeedHook}).
 * Module composition lives in the entry code
 * ({@link PointApp}) as a {@link com.jujin.freeway.ioc.ModuleNode} tree.
 *
 * Schema is auto-migrated via SchemaEntity contribution (not manual Schema.ensure).
 * Database health check is registered as an example of freeway's HealthCheck extension.
 */
public class PointModule implements ModuleEx {
    private static final Logger log = LoggerFactory.getLogger(PointModule.class);

    @Override
    public void bind(Binder binder) {
        // ── 1. Schema auto-migration via SchemaEntity ──
        // DbModule will automatically run Schema.ensure at startup for all contributed entities.
        binder
            .contribute(SchemaEntity.class)
            .add(SchemaEntity.of("bbs", ALL_ENTITIES));

        // ── 2. Database health check ──
        // Pluggable HealthCheck: freeway's built-in /healthz endpoint returns {"status":"ok"}.
        // This custom check adds database connectivity status.
        binder
            .bind(HealthCheck.class)
            .to(container -> () -> {
                try {
                    var db = container.get(Database.class);
                    db.query("SELECT 1").list(Row.class);
                    return Map.of("status", "ok", "db", "connected");
                } catch (Exception e) {
                    return Map.of("status", "degraded", "db", "unreachable");
                }
            })
            .primary();

        // ── 3. Seed default roles & permissions (idempotent) ──
        binder
            .contribute(RuntimeHook.class)
            .add("data-seed", new DataSeedHook());

        // ── 4. Seed dev test data (idempotent, dev profiles only) ──
        binder
            .contribute(RuntimeHook.class)
            .add("dev-data-seed", new DevDataSeedHook());
    }

    /** All entity classes for Schema.ensure auto-migration. */
    // Entities actively managed by Schema.ensure. The legacy bbs-go tables
    // without any code (Vote*, CheckIn, Badge*, LevelConfig, TaskConfig,
    // Dict*, SmsCode, EmailCode/Log, UserReport, ForbiddenWord, UserFeed,
    // UserToken, UserExpLog/ScoreLog, UserTask*, Link) are intentionally left
    // OUT — their tables stay in the DB untouched, and they no longer
    // participate in schema migration. Re-add when a feature uses them.
    static final Class<?>[] ALL_ENTITIES = {
        User.class,
        Topic.class,
        Comment.class,
        Article.class,
        Category.class,
        Tag.class,
        TopicTag.class,
        ArticleTag.class,
        UserLike.class,
        Favorite.class,
        UserFollow.class,
        Message.class,
        OperateLog.class,
        SysConfig.class,
        Attachment.class,
        AttachmentDownloadLog.class,
        ThirdUser.class,
        Role.class,
        Permission.class,
        RolePermission.class,
        UserRole.class,
    };
}
