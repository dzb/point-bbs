package com.jujin.point.web;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.entity.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enriches entities with related user info for frontend display.
 */
public final class ResponseEnricher {

    // -- single-item (for detail endpoints) --

    public static Map<String, Object> enrichTopic(Topic t) {
        return buildTopic(t, queryUser(t.getUserId()));
    }

    public static Map<String, Object> enrichComment(Comment c) {
        return buildComment(c, queryUser(c.getUserId()));
    }

    public static Map<String, Object> enrichArticle(Article a) {
        return buildArticle(a, queryUser(a.getUserId()), queryTags(a.getId()));
    }

    // -- batch (for list endpoints) --

    public static List<Map<String, Object>> enrichTopics(List<Topic> topics) {
        var users = batchQueryUsers(topics.stream().map(Topic::getUserId).collect(Collectors.toSet()));
        return topics.stream().map(t -> buildTopic(t, users.get(t.getUserId()))).toList();
    }

    public static List<Map<String, Object>> enrichComments(List<Comment> comments) {
        var users = batchQueryUsers(comments.stream().map(Comment::getUserId).collect(Collectors.toSet()));
        return comments.stream().map(c -> buildComment(c, users.get(c.getUserId()))).toList();
    }

    public static List<Map<String, Object>> enrichArticles(List<Article> articles) {
        var userIds = articles.stream().map(Article::getUserId).collect(Collectors.toSet());
        var users = batchQueryUsers(userIds);
        // Batch query tags for all article IDs
        var articleIds = articles.stream().map(Article::getId).toList();
        var tagsMap = batchQueryTags(articleIds);
        return articles.stream().map(a -> buildArticle(a, users.get(a.getUserId()), tagsMap.getOrDefault(a.getId(), List.of()))).toList();
    }

    // -- builders --

    private static Map<String, Object> buildTopic(Topic t, Map<String, Object> user) {
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
        m.put("imageList", t.getImageList());
        m.put("hideContent", t.getHideContent());
        m.put("qaStatus", t.getQaStatus());
        m.put("bountyScore", t.getBountyScore());
        m.put("user", user);
        return m;
    }

    private static Map<String, Object> buildComment(Comment c, Map<String, Object> user) {
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
        m.put("imageList", c.getImageList());
        m.put("user", user);
        return m;
    }

    private static Map<String, Object> buildArticle(Article a, Map<String, Object> user, List<String> tags) {
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
        m.put("user", user);
        m.put("tags", tags);
        return m;
    }

    // -- batch queries --

    private static Map<Long, Map<String, Object>> batchQueryUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        var placeholders = userIds.stream().map(id -> "?").collect(Collectors.joining(","));
        var params = userIds.toArray();
        var rows = AppContext.get(Database.class)
            .query("SELECT id, nickname, username, avatar FROM bbs_user WHERE id IN (" + placeholders + ")", params)
            .list(Row.class);
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (var r : rows) {
            var m = new LinkedHashMap<String, Object>();
            m.put("id", r.longVal("id"));
            m.put("nickname", r.string("nickname"));
            m.put("username", r.string("username"));
            m.put("avatar", r.string("avatar"));
            result.put(r.longVal("id"), m);
        }
        return result;
    }

    private static Map<Long, List<String>> batchQueryTags(List<Long> articleIds) {
        if (articleIds.isEmpty()) return Map.of();
        var placeholders = articleIds.stream().map(id -> "?").collect(Collectors.joining(","));
        var params = articleIds.toArray();
        var rows = AppContext.get(Database.class)
            .query("SELECT at.article_id, t.name FROM bbs_tag t INNER JOIN bbs_article_tag at ON t.id = at.tag_id WHERE at.article_id IN (" + placeholders + ") AND at.status = 1", params)
            .list(Row.class);
        Map<Long, List<String>> result = new HashMap<>();
        for (var r : rows) {
            long aid = r.longVal("article_id");
            result.computeIfAbsent(aid, k -> new ArrayList<>()).add(r.string("name"));
        }
        return result;
    }

    private static Map<String, Object> queryUser(long userId) {
        var rows = AppContext.get(Database.class)
            .query("SELECT id, nickname, username, avatar FROM bbs_user WHERE id = ?", userId)
            .list(Row.class);
        if (rows.isEmpty()) return new LinkedHashMap<>();
        var r = rows.getFirst();
        var m = new LinkedHashMap<String, Object>();
        m.put("id", r.longVal("id"));
        m.put("nickname", r.string("nickname"));
        m.put("username", r.string("username"));
        m.put("avatar", r.string("avatar"));
        return m;
    }

    private static List<String> queryTags(long articleId) {
        return AppContext.get(Database.class)
            .query("SELECT t.name FROM bbs_tag t INNER JOIN bbs_article_tag at ON t.id = at.tag_id WHERE at.article_id = ? AND at.status = 1", articleId)
            .list(Row.class).stream().map(r -> r.string("name")).toList();
    }
}
