package com.jujin.point.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.PoolConfig;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.point.domain.entity.SysConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the reserved-word column in {@code bbs_sys_config}.
 *
 * <p>The schema creates the column quoted and therefore lowercase
 * ({@code "key"}); an unquoted reference in the service folded to {@code KEY}
 * and every write failed with "Column \"KEY\" not found" — the admin config
 * screen returned 500 on a brand-new database. These tests exercise insert,
 * in-place update and reload against the same schema the application builds.</p>
 */
class SysConfigServiceTest {

    private static Database db;
    private static SysConfigService svc;

    @BeforeAll
    static void setUp() {
        db = Database.create(PoolConfig.defaults(
            "jdbc:h2:mem:point_sysconfig_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
                + ";NON_KEYWORDS=VALUE,KEY,LABEL,TYPE,LEVEL,INDEX",
            "sa", ""));
        Schema.ensure(db, new Class<?>[] { SysConfig.class });
        svc = new SysConfigService(db);
    }

    @AfterAll
    static void tearDown() throws Exception {
        db.close();
    }

    @Test
    void setInsertsThenReadsBack() {
        svc.set("site.name", "point");
        assertEquals("point", svc.get("site.name"));
        assertEquals("point", svc.getOrDefault("site.name", "fallback"));
    }

    @Test
    void setUpdatesTheExistingRowInsteadOfDuplicatingIt() {
        svc.set("site.title", "聚");
        svc.set("site.title", "聚·point");

        assertEquals("聚·point", svc.get("site.title"));
        long rows = db.query("SELECT COUNT(*) AS cnt FROM bbs_sys_config WHERE \"key\" = 'site.title'")
            .one(Row.class).orElseThrow().asLong("cnt");
        assertEquals(1, rows, "a second set must update the row, not insert another");
    }

    @Test
    void persistedRowsSurviveAReload() {
        svc.set("site.icp", "沪ICP备00000000号");
        svc.reload();

        assertEquals("沪ICP备00000000号", svc.get("site.icp"),
            "the loader must map the quoted key column back onto the entity");
        assertNull(svc.get("site.absent"));
    }
}
