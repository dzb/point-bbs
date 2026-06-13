package com.jujin.point.web;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.entity.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import java.util.*;

/**
 * Enriches entities with related user info for frontend display.
 */
public final class ResponseEnricher {

    public static Map<String, Object> enrichTopic(Topic t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("type", t.getType());
        m.put("title", t.getTitle());
        m.put("content", t.getContent());
        m.put("contentType", t.getContentType());
        m.put("categoryId", t.getCategoryId());
        m.put("userId", t.getUserId());
        m.put("viewCount", t.getViewCount());
        m.put("commentCount", t.getCommentCount());
        m.put("likeCount", t.getLikeCount());
        m.put("status", t.getStatus());
        m.put("sticky", t.isSticky());
        m.put("recommend", t.isRecommend());
        m.put("createTime", t.getCreateTime());
        m.put("lastCommentTime", t.getLastCommentTime());
        m.put("user", queryUser(t.getUserId()));
        return m;
    }

    public static Map<String, Object> enrichComment(Comment c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("entityType", c.getEntityType());
        m.put("entityId", c.getEntityId());
        m.put("content", c.getContent());
        m.put("contentType", c.getContentType());
        m.put("quoteId", c.getQuoteId());
        m.put("likeCount", c.getLikeCount());
        m.put("commentCount", c.getCommentCount());
        m.put("status", c.getStatus());
        m.put("createTime", c.getCreateTime());
        m.put("user", queryUser(c.getUserId()));
        return m;
    }

    public static Map<String, Object> enrichArticle(Article a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("title", a.getTitle());
        m.put("summary", a.getSummary());
        m.put("content", a.getContent());
        m.put("contentType", a.getContentType());
        m.put("cover", a.getCover());
        m.put("sourceUrl", a.getSourceUrl());
        m.put("viewCount", a.getViewCount());
        m.put("commentCount", a.getCommentCount());
        m.put("likeCount", a.getLikeCount());
        m.put("userId", a.getUserId());
        m.put("createTime", a.getCreateTime());
        m.put("user", queryUser(a.getUserId()));
        return m;
    }

    private static Map<String, Object> queryUser(long userId) {
        var rows = AppContext.get(Database.class)
            .query("SELECT id, nickname, avatar FROM bbs_user WHERE id = ?", userId)
            .list(Row.class);
        if (rows.isEmpty()) return new LinkedHashMap<>();
        var r = rows.getFirst();
        var m = new LinkedHashMap<String, Object>();
        m.put("id", r.integer("id"));
        m.put("nickname", r.string("nickname"));
        m.put("avatar", r.string("avatar"));
        return m;
    }
}
