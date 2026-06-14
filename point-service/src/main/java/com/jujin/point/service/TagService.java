package com.jujin.point.service;

import com.jujin.point.domain.entity.Tag;
import com.jujin.freeway.db.Database;

import java.util.List;

/**
 * Tag service.
 */
public class TagService {
    private final Database db;

    public TagService(Database db) {
        this.db = db;
    }

    public List<Tag> findAll() {
        return db.query("SELECT * FROM bbs_tag WHERE status = 1 ORDER BY create_time DESC")
            .list(Tag.class);
    }

    public Tag getOrCreate(String name) {
        var existing = db.query("SELECT * FROM bbs_tag WHERE name = $name").param("name", name)
            .one(Tag.class);
        if (existing.isPresent()) return existing.get();

        var tag = new Tag();
        var now = System.currentTimeMillis();
        tag.setName(name);
        tag.setStatus(1);
        tag.setCreateTime(now);
        tag.setUpdateTime(now);

        db.execute("INSERT INTO bbs_tag (name, status, create_time, update_time) VALUES (?, 1, ?, ?)",
            name, now, now);
        return db.query("SELECT * FROM bbs_tag WHERE name = $name").param("name", name).one(Tag.class).orElseThrow();
    }
}
