package com.jujin.point.service;

import com.jujin.point.domain.entity.Tag;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    /**
     * Tag names per article — one IN query for a whole feed page instead of a
     * query per article. Empty input returns an empty map ({@code IN ()} is a
     * syntax error on both dialects).
     */
    public Map<Long, List<String>> findNamesByArticleIds(Collection<Long> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        var placeholders = articleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        var rows = db.query(
            "SELECT at.article_id, t.name FROM bbs_tag t " +
            "INNER JOIN bbs_article_tag at ON t.id = at.tag_id " +
            "WHERE at.article_id IN (" + placeholders + ") AND at.status = 1",
            articleIds.toArray()
        ).list(Row.class);
        Map<Long, List<String>> result = new HashMap<>();
        for (var row : rows) {
            result.computeIfAbsent(row.asLong("article_id"), k -> new ArrayList<>())
                .add(row.asString("name"));
        }
        return result;
    }

    public Tag getOrCreate(String name) {
        var existing = db.query("SELECT * FROM bbs_tag WHERE name = $name").bind("name", name)
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
        return db.query("SELECT * FROM bbs_tag WHERE name = $name").bind("name", name).one(Tag.class).orElseThrow();
    }
}
