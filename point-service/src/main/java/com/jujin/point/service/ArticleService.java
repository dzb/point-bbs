package com.jujin.point.service;

import com.jujin.point.domain.dto.ArticleDtos.UpdateArticleRequest;
import com.jujin.point.domain.entity.Article;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;

import java.util.List;
import java.util.Optional;

/**
 * Article service.
 */
public class ArticleService {
    private final Database db;
    private final TagService tagSvc;
    private final MentionNotifier mentionSvc;

    public ArticleService(Database db, TagService tagSvc, MentionNotifier mentionSvc) {
        this.db = db;
        this.tagSvc = tagSvc;
        this.mentionSvc = mentionSvc;
    }

    public Optional<Article> findById(long id) {
        return db.query("SELECT * FROM bbs_article WHERE id = $id AND status = 1").param("id", id).one(Article.class);
    }

    public Article create(long userId, String title, String summary, String content, String contentType) {
        var now = System.currentTimeMillis();
        var article = new Article();
        article.setUserId(userId);
        article.setTitle(title);
        article.setSummary(summary);
        article.setContent(content);
        article.setContentType(contentType != null ? contentType : "markdown");
        article.setStatus(1);
        article.setCreateTime(now);
        article.setUpdateTime(now);

        db.transaction(() -> {
            var result = db.execute(
                "INSERT INTO bbs_article (user_id, title, summary, content, content_type, status, view_count, comment_count, like_count, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, 1, 0, 0, 0, ?, ?)",
                userId, title, summary, content, contentType != null ? contentType : "markdown", now, now
            );
            if (result.hasGeneratedKey()) {
                article.setId(result.longKey());
            }

            mentionSvc.notifyMentions(userId, content, "article", article.getId(), now);
        });

        return article;
    }

    public List<Article> getRecent(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return db.query("SELECT * FROM bbs_article WHERE status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset).list(Article.class);
    }

    public void update(long id, UpdateArticleRequest req) {
        // Partial update: only persist fields the client actually sent, so a
        // partial edit cannot wipe summary/content/cover/sourceUrl.
        var sets = new java.util.ArrayList<String>();
        var params = new java.util.ArrayList<Object>();
        if (req.title() != null) { sets.add("title=?"); params.add(req.title()); }
        if (req.summary() != null) { sets.add("summary=?"); params.add(req.summary()); }
        if (req.content() != null) { sets.add("content=?"); params.add(req.content()); }
        if (req.contentType() != null) { sets.add("content_type=?"); params.add(req.contentType()); }
        if (req.cover() != null) { sets.add("cover=?"); params.add(req.cover()); }
        if (req.sourceUrl() != null) { sets.add("source_url=?"); params.add(req.sourceUrl()); }
        if (sets.isEmpty()) return;
        params.add(System.currentTimeMillis());
        params.add(id);
        db.execute(
            "UPDATE bbs_article SET " + String.join(", ", sets) + ", update_time=? WHERE id=?",
            params.toArray()
        );
        if (req.tags() != null) updateTags(id, req.tags());
    }

    public void updateCover(long id, String cover) {
        db.execute("UPDATE bbs_article SET cover=? WHERE id=?", cover, id);
    }

    public void updateTags(long articleId, java.util.List<String> tags) {
        // Clear existing tags
        db.execute("DELETE FROM bbs_article_tag WHERE article_id = ?", articleId);
        // Add new tags via TagService
        long now = System.currentTimeMillis();
        for (String tagName : tags) {
            var tag = tagSvc.getOrCreate(tagName);
            db.execute("INSERT INTO bbs_article_tag (article_id, tag_id, status, create_time) VALUES (?, ?, 1, ?)",
                articleId, tag.getId(), now);
        }
    }

    public void updateSourceUrl(long id, String sourceUrl) {
        db.execute("UPDATE bbs_article SET source_url=? WHERE id=?", sourceUrl, id);
    }

    public List<Article> getByUser(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_article WHERE user_id = $userId AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("userId", userId).param("limit", pageSize).param("offset", offset).list(Article.class);
    }

    public long countRecent() {
        var row = db.query("SELECT COUNT(*) AS cnt FROM bbs_article WHERE status = 1")
            .one(Row.class).orElse(null);
        return row != null ? row.longValue("cnt") : 0;
    }

    public long countByUser(long userId) {
        var row = db.query("SELECT COUNT(*) AS cnt FROM bbs_article WHERE user_id = $userId AND status = 1")
            .param("userId", userId).one(Row.class).orElse(null);
        return row != null ? row.longValue("cnt") : 0;
    }

    public void delete(long userId, long articleId) {
        var article = findById(articleId)
            .orElseThrow(() -> new ServiceException("文章不存在"));
        if (article.getUserId() != userId) {
            throw new ServiceException("无权删除");
        }
        db.execute("UPDATE bbs_article SET status = 0 WHERE id = ?", articleId);
    }
}
