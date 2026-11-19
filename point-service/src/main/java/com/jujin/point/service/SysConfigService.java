package com.jujin.point.service;

import com.jujin.point.db.repository.*;
import com.jujin.point.domain.entity.SysConfig;
import com.jujin.freeway.db.Database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dynamic system configuration service — DB-backed with in-memory cache.
 * The whole table is loaded once and served from memory; writes update
 * the row and the cache together.
 */
public class SysConfigService {
    private final Database db;
    private final Map<String, String> memoryCache = new ConcurrentHashMap<>();
    private volatile boolean loaded = false;

    public SysConfigService(Database db) {
        this.db = db;
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            var configs = db.query("SELECT * FROM bbs_sys_config").list(SysConfig.class);
            for (var c : configs) {
                memoryCache.put(c.getKey(), c.getValue());
            }
            loaded = true;
        }
    }

    public String get(String key) {
        ensureLoaded();
        return memoryCache.get(key);
    }

    public String getOrDefault(String key, String defaultValue) {
        var value = get(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        var value = get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBool(String key, boolean defaultValue) {
        var value = get(key);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    public void set(String key, String value) {
        db.transaction(() -> {
            // `key` is a reserved word, so the schema creates the column
            // QUOTED — and quoted means case-sensitive: the column really is
            // lowercase "key". An unquoted reference folds to KEY (H2/Postgres
            // uppercase unquoted identifiers here) and fails with
            // "Column \"KEY\" not found". Quote it to match the schema.
            var existing = db.query("SELECT * FROM bbs_sys_config WHERE \"key\" = $key").bind("key", key)
                .one(SysConfig.class);
            if (existing.isPresent()) {
                var c = existing.get();
                c.setValue(value);
                c.setUpdateTime(System.currentTimeMillis());
                db.execute("UPDATE bbs_sys_config SET value = ?, update_time = ? WHERE id = ?",
                    value, System.currentTimeMillis(), c.getId());
            } else {
                var c = new SysConfig();
                c.setKey(key);
                c.setValue(value);
                c.setName(key);
                c.setCreateTime(System.currentTimeMillis());
                c.setUpdateTime(System.currentTimeMillis());
                db.execute("INSERT INTO bbs_sys_config (\"key\", value, name, create_time, update_time) VALUES (?, ?, ?, ?, ?)",
                    key, value, key, System.currentTimeMillis(), System.currentTimeMillis());
            }
        });
        memoryCache.put(key, value);
    }

    public Map<String, String> getAll() {
        ensureLoaded();
        return Map.copyOf(memoryCache);
    }

    public void reload() {
        loaded = false;
        memoryCache.clear();
        ensureLoaded();
    }
}
