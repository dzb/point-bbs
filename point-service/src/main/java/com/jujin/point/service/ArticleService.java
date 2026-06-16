package com.jujin.point.service;

import com.jujin.point.domain.entity.Article;
import com.jujin.point.domain.event.UserMentionedEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.ioc.EventBus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Article service.
 */
public class ArticleService {
    private final Database db;
    private final TagService tagSvc;
    private final EventBus eventBus;

    public ArticleService(Database db, TagService tagSvc, EventBus eventBus) {
        this.db = db;
        this.tagSvc = tagSvc;
        this.eventBus = eventBus;
    }

    public Optional<Article> findById(long id) {
        return db.query("SELECT * FROM bbs_article WHERE id = $id AND status = 1").param("id", id).one(Article.class);
    }

    public Article create(long userId, String title, String summary, String content, String contentType) {
        var now = System.currentTimeMillis();
        var result = new Article[1];

        db.transaction(() -> {
            db.execute(
                "INSERT INTO bbs_article (user_id, title, summary, content, content_type, status, view_count, comment_count, like_count, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, 1, 0, 0, 0, ?, ?)",
                userId, title, summary, content, contentType != null ? contentType : "markdown", now, now
            );
            var article = db.query("SELECT * FROM bbs_article WHERE user_id = ? ORDER BY create_time DESC LIMIT 1", userId)
                .one(Article.class).orElseThrow();
            result[0] = article;

            // Notify @mentioned users
            var mentioned = MentionParser.extractMentions(content);
            var notified = new HashSet<Long>();
            for (String username : mentioned) {
                db.query("SELECT id FROM bbs_user WHERE username = ? AND status <> 0", username)
                    .one(Row.class).ifPresent(row -> {
                        long uid = row.longVal("id");
                        if (uid != userId && notified.add(uid)) {
                            eventBus.publish(new UserMentionedEvent(userId, uid,
                                "article", article.getId(), truncate(content, 100), now));
                        }
                    });
            }
        });

        return result[0];
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    public List<Article> getRecent(int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return db.query("SELECT * FROM bbs_article WHERE status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset).list(Article.class);
    }

    public void update(long id, String title, String summary, String content,
                       String contentType, String cover, String sourceUrl) {
        db.execute(
            "UPDATE bbs_article SET title=?, summary=?, content=?, content_type=?, cover=?, source_url=?, update_time=? WHERE id=?",
            title, summary, content, contentType, cover, sourceUrl, System.currentTimeMillis(), id);
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
        int offset = (page - 1) * pageSize;
        return db.query(
            "SELECT * FROM bbs_article WHERE user_id = $userId AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("userId", userId).param("limit", pageSize).param("offset", offset).list(Article.class);
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
