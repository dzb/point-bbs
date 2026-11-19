package com.jujin.point.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jujin.freeway.boot.AppRuntime;
import com.jujin.freeway.boot.FreewayApp;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.http.HttpModule;
import com.jujin.point.service.DbQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Demo data is an explicit dev-profile decision.
 *
 * <p>The seed ships known passwords ("123456"), so a bare {@code java -jar}
 * — no profile — must leave the database empty: it used to look like a normal
 * deployment while silently creating demo users and an admin account. Roles
 * and permissions (and the config-gated bootstrap admin) still come from
 * {@code DataSeedHook}, which is what every deployment needs.</p>
 */
class SeedProfileTest {

    private AppRuntime app;

    @AfterEach
    void stop() {
        if (app != null) {
            app.close();
            app = null;
        }
        System.clearProperty(HttpModule.ConfigKeys.SERVER_PORT);
        System.clearProperty(HttpModule.ConfigKeys.SERVER_HOST);
        System.clearProperty("freeway.db.url");
        System.clearProperty("bbs.jwt.secret");
    }

    @Test
    void noProfileStartsWithoutDemoData() throws Exception {
        app = start("point_seed_none", null);
        assertEquals(0, countUsers(app), "no profile must not seed demo accounts");
        assertTrue(countPermissions(app) > 0, "roles and permissions still seed — deploys need them");
    }

    @Test
    void devProfileSeedsDemoData() throws Exception {
        app = start("point_seed_dev", "dev");
        assertTrue(countUsers(app) > 0, "an explicit dev profile still seeds the demo site");
    }

    private static AppRuntime start(String dbName, String profile) {
        System.setProperty(HttpModule.ConfigKeys.SERVER_PORT, "0");
        System.setProperty(HttpModule.ConfigKeys.SERVER_HOST, "127.0.0.1");
        System.setProperty(
            "freeway.db.url",
            "jdbc:h2:mem:" + dbName + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX"
        );
        System.setProperty("bbs.jwt.secret", "test-secret-not-a-placeholder-1234567890");
        var launcher = FreewayApp.create(PointModules.base())
            .name("point-seed-test")
            .autoDiscovery(false);
        if (profile != null) {
            launcher.args("--F2.profile=" + profile);
        }
        return launcher.start();
    }

    private static long countUsers(AppRuntime app) {
        return DbQuery.count(app.get(Database.class), "SELECT COUNT(*) AS cnt FROM bbs_user");
    }

    private static long countPermissions(AppRuntime app) {
        return DbQuery.count(app.get(Database.class), "SELECT COUNT(*) AS cnt FROM bbs_permission");
    }
}
