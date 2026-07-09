package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.Topic;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.Row;

import java.util.List;

/**
 * Topic repository.
 */
public class TopicRepository extends BaseRepository<Topic> {

    public TopicRepository(Database db, Orm orm) {
        super(db, orm, Topic.class);
    }

    public List<Topic> findByUserId(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE user_id = $userId AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("userId", userId).param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findByCategoryId(long categoryId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE category_id = $catId AND status = 1 ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .param("catId", categoryId).param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findRecentTopics(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = 0 ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findTweets(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findRecommended(int limit) {
        return query(
            "SELECT * FROM bbs_topic WHERE recommend = TRUE AND status = 1 ORDER BY recommend_time DESC LIMIT $limit")
            .param("limit", limit)
            .list(Topic.class);
    }

    public List<Topic> findSticky() {
        return query(
            "SELECT * FROM bbs_topic WHERE sticky = TRUE AND status = 1 ORDER BY sticky_time DESC"
        ).list(Topic.class);
    }

    public List<Topic> findByTagId(long tagId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT t.* FROM bbs_topic t INNER JOIN bbs_topic_tag tt ON t.id = tt.topic_id " +
            "WHERE tt.tag_id = $tagId AND t.status = 1 ORDER BY t.last_comment_time DESC LIMIT $limit OFFSET $offset")
            .param("tagId", tagId).param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> searchByTitle(String keyword, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        var escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND title LIKE $keyword ESCAPE '\\' ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .param("keyword", "%" + escaped + "%").param("limit", pageSize).param("offset", offset)
            .list(Topic.class);
    }

    public long countByUserId(long userId) {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_topic WHERE user_id = $userId AND status = 1")
            .param("userId", userId).one(Row.class).orElse(null);
        return row != null ? row.longVal("cnt") : 0;
    }

    public long countByCategoryId(long categoryId) {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_topic WHERE category_id = $catId AND status = 1")
            .param("catId", categoryId).one(Row.class).orElse(null);
        return row != null ? row.longVal("cnt") : 0;
    }

    public long countByTitleSearch(String keyword) {
        var escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_topic WHERE status = 1 AND title LIKE $keyword ESCAPE '\\'")
            .param("keyword", "%" + escaped + "%")
            .one(Row.class).orElse(null);
        return row != null ? row.longVal("cnt") : 0;
    }

    public int incrViewCount(long topicId) {
        var result = execute("UPDATE bbs_topic SET view_count = view_count + 1 WHERE id = ?", topicId);
        return result;
    }

    public void updateLastComment(long topicId, long userId, long time) {
        execute("UPDATE bbs_topic SET comment_count = comment_count + 1, last_comment_time = ?, last_comment_user_id = ? WHERE id = ?",
            time, userId, topicId);
    }
}
