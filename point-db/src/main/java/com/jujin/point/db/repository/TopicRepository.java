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
            .bind("userId", userId).bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    /**
     * Feed of the people {@code userId} follows. The follow set is a
     * subquery, so an empty follow list needs no special case and the whole
     * feed stays one statement.
     */
    public List<Topic> findFollowingFeed(long userId, int type, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = $type AND user_id IN (" +
            "SELECT other_id FROM bbs_user_follow WHERE user_id = $userId AND status = 1) " +
            "ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .bind("type", type).bind("userId", userId)
            .bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    public long countFollowingFeed(long userId, int type) {
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_topic WHERE status = 1 AND type = $type AND user_id IN (" +
            "SELECT other_id FROM bbs_user_follow WHERE user_id = $userId AND status = 1)")
            .bind("type", type).bind("userId", userId)
            .one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    /** Moments: type 1 regardless of author, newest first. */
    public List<Topic> findByType(int type, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE type = $type AND status = 1 ORDER BY create_time DESC LIMIT $limit OFFSET $offset")
            .bind("type", type).bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findByCategoryId(long categoryId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE category_id = $catId AND status = 1 ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .bind("catId", categoryId).bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> findRecentTopics(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = 0 ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }


    public List<Topic> findRecommended(int limit) {
        return query(
            "SELECT * FROM bbs_topic WHERE recommend = TRUE AND status = 1 ORDER BY recommend_time DESC LIMIT $limit")
            .bind("limit", limit)
            .list(Topic.class);
    }


    public List<Topic> findByTagId(long tagId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT t.* FROM bbs_topic t INNER JOIN bbs_topic_tag tt ON t.id = tt.topic_id " +
            "WHERE tt.tag_id = $tagId AND t.status = 1 ORDER BY t.last_comment_time DESC LIMIT $limit OFFSET $offset")
            .bind("tagId", tagId).bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    public List<Topic> searchByTitle(String keyword, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        // ESCAPE '!' — a dialect-neutral escape char (H2 PostgreSQL-mode and
        // real PostgreSQL both accept it; no backslash-lexing ambiguity).
        var escaped = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND title LIKE $keyword ESCAPE '!' ORDER BY last_comment_time DESC LIMIT $limit OFFSET $offset")
            .bind("keyword", "%" + escaped + "%").bind("limit", pageSize).bind("offset", offset)
            .list(Topic.class);
    }

    public long countByUserId(long userId) {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_topic WHERE user_id = $userId AND status = 1")
            .bind("userId", userId).one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    public long countByCategoryId(long categoryId) {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_topic WHERE category_id = $catId AND status = 1")
            .bind("catId", categoryId).one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    public long countByType(int type) {
        var row = query("SELECT COUNT(*) AS cnt FROM bbs_topic WHERE type = $type AND status = 1")
            .bind("type", type).one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    public long countByTitleSearch(String keyword) {
        var escaped = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        var row = query(
            "SELECT COUNT(*) AS cnt FROM bbs_topic WHERE status = 1 AND title LIKE $keyword ESCAPE '!'")
            .bind("keyword", "%" + escaped + "%")
            .one(Row.class).orElse(null);
        return row != null ? row.asLong("cnt") : 0;
    }

    public int incrViewCount(long topicId) {
        var result = execute("UPDATE bbs_topic SET view_count = view_count + 1 WHERE id = ?", topicId);
        return result;
    }

}
