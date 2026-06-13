package com.jujin.point.boot;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.entity.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.Module;
import com.jujin.freeway.ioc.RuntimeHook;

/**
 * Primary bbs module — composes all feature modules and initializes AppContext.
 *
 * Pass this to Launcher.run(PointModule.class, args).
 * Freeway's DbModule and HttpModule are auto-discovered via ServiceLoader.
 */
public class PointModule implements Module {

    @Override
    public void bind(Binder binder) {
        // Sub-modules (ServiceModule, WebModule, AdminWebModule, CacheModule)
        // are auto-discovered via ServiceLoader — no manual bind() needed.

        // 1. Auto-migrate schema (freeway Schema.ensure)
        binder.contribute(RuntimeHook.class)
            .add("schema-migration", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var db = container.get(Database.class);
                    Schema.ensure(db, ALL_ENTITIES);
                }
                @Override
                public void stop(Container container) {}
            }).before("app-context-init");

        // 2. Seed default roles & permissions (idempotent)
        binder.contribute(RuntimeHook.class)
            .add("data-seed", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var db = container.get(Database.class);
                    long count = com.jujin.point.service.DbQuery.count(db,
                        "SELECT COUNT(*) AS cnt FROM bbs_permission", (Object[]) new Object[0]);
                    if (count == 0) {
                        long now = System.currentTimeMillis();
                        // Create default permissions
                        String[][] perms = {
                            {"admin", "topic:manage", "帖子管理", "内容"},
                            {"admin", "user:manage", "用户管理", "用户"},
                            {"admin", "config:manage", "配置管理", "系统"},
                            {"admin", "article:manage", "文章管理", "内容"},
                        };
                        for (String[] p : perms) {
                            db.execute("INSERT INTO bbs_permission (type, code, name, group_name, sort_no, status, create_time, update_time) VALUES (?,?,?,?,0,1,?,?)",
                                (Object) p[0], (Object) p[1], (Object) p[2], (Object) p[3], now, now);
                        }
                        // Create admin role
                        db.execute("INSERT INTO bbs_role (type, name, code, sort_no, status, create_time, update_time) VALUES (0,'管理员','admin',0,1,?,?)", now, now);
                        // Assign all permissions to admin role
                        var rows = db.query("SELECT id FROM bbs_permission").list(com.jujin.freeway.db.Row.class);
                        for (var r : rows) {
                            long pid = r.integer("id").longValue();
                            db.execute("INSERT INTO bbs_role_permission (role_id, permission_id, create_time) VALUES (1,?,?)", pid, now);
                        }
                    }
                }
                @Override
                public void stop(Container container) {}
            }).before("app-context-init");

        // 3. Initialize AppContext AFTER all services are bound
        //    (runs before HTTP server starts accepting requests)
        binder.contribute(RuntimeHook.class)
            .add("app-context-init", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    AppContext.init(container);
                }

                @Override
                public void stop(Container container) {}
            }).before("freeway.http.server");
    }

    /** All entity classes for Schema.ensure auto-migration. */
    static final Class<?>[] ALL_ENTITIES = {
        User.class, Topic.class, Comment.class, Article.class, Category.class,
        Tag.class, TopicTag.class, ArticleTag.class,
        Vote.class, VoteOption.class, VoteRecord.class,
        UserLike.class, Favorite.class, UserFollow.class, Message.class,
        CheckIn.class, UserScoreLog.class, UserExpLog.class, OperateLog.class,
        UserTaskEvent.class, UserTaskLog.class, TaskConfig.class,
        Badge.class, UserBadge.class, LevelConfig.class,
        SysConfig.class, Link.class, ForbiddenWord.class,
        Attachment.class, AttachmentDownloadLog.class,
        UserFeed.class, UserReport.class,
        EmailCode.class, EmailLog.class, SmsCode.class,
        UserToken.class, ThirdUser.class,
        Role.class, Permission.class, RolePermission.class, UserRole.class,
        DictType.class, Dict.class
    };
}
