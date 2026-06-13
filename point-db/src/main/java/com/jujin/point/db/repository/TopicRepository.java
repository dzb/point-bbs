package com.jujin.point.db.repository;

import com.jujin.point.domain.entity.Topic;
import com.jujin.freeway.db.Database;

import java.util.List;

/**
 * Topic repository.
 */
public class TopicRepository extends BaseRepository<Topic> {

    public TopicRepository(Database db) {
        super(db, Topic.class);
    }

    public List<Topic> findByUserId(long userId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE user_id = ? AND status = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
            userId, pageSize, offset
        ).list(Topic.class);
    }

    public List<Topic> findByCategoryId(long categoryId, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE category_id = ? AND status = 1 ORDER BY last_comment_time DESC LIMIT ? OFFSET ?",
            categoryId, pageSize, offset
        ).list(Topic.class);
    }

    public List<Topic> findRecentTopics(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = 0 ORDER BY last_comment_time DESC LIMIT ? OFFSET ?",
            pageSize, offset
        ).list(Topic.class);
    }

    public List<Topic> findTweets(int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND type = 1 ORDER BY create_time DESC LIMIT ? OFFSET ?",
            pageSize, offset
        ).list(Topic.class);
    }

    public List<Topic> findRecommended(int limit) {
        return query(
            "SELECT * FROM bbs_topic WHERE recommend = TRUE AND status = 1 ORDER BY recommend_time DESC LIMIT ?",
            limit
        ).list(Topic.class);
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
            "WHERE tt.tag_id = ? AND t.status = 1 ORDER BY t.last_comment_time DESC LIMIT ? OFFSET ?",
            tagId, pageSize, offset
        ).list(Topic.class);
    }

    public List<Topic> searchByTitle(String keyword, int page, int pageSize) {
        long offset = (long) (page - 1) * pageSize;
        return query(
            "SELECT * FROM bbs_topic WHERE status = 1 AND title LIKE ? ORDER BY last_comment_time DESC LIMIT ? OFFSET ?",
            "%" + keyword + "%", pageSize, offset
        ).list(Topic.class);
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
