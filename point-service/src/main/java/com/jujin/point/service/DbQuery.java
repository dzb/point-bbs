package com.jujin.point.service;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;

/**
 * Shared query helpers.
 */
public final class DbQuery {
    private DbQuery() {}

    /** Execute a COUNT(*) AS cnt query and return the count. */
    public static long count(Database db, String sql, Object... params) {
        var rows = db.query(sql, params).list(Row.class);
        if (rows.isEmpty()) return 0;
        Long cnt = rows.getFirst().get("cnt", Long.class);
        return cnt != null ? cnt : 0;
    }

    /** Get a single long from a query returning one row. */
    public static Long longValue(Database db, String sql, String col, Object... params) {
        var rows = db.query(sql, params).list(Row.class);
        if (rows.isEmpty()) return null;
        return rows.getFirst().get(col, Long.class);
    }
}
