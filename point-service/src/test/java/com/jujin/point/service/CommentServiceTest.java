package com.jujin.point.service;

import static org.junit.jupiter.api.Assertions.*;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.DatabaseBuilder;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.PoolConfig;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.EventBus;
import com.jujin.freeway.ioc.Freeway;
import com.jujin.point.db.repository.CommentRepository;
import com.jujin.point.db.repository.TopicRepository;
import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.entity.Comment;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.entity.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CommentServiceTest {

    private static Container container;
    private static CommentService commentService;
    private static TopicService topicService;
    private static Database db;

    @BeforeAll
    static void setUp() {
        var config = PoolConfig.defaults(
            "jdbc:h2:mem:point_comment_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        db = DatabaseBuilder.from(config).build();
        var orm = Orm.of(db);
        Schema.ensure(db, new Class<?>[] { Topic.class, Comment.class, User.class });

        container = Freeway.create(binder -> {
            binder.bind(Database.class).to(db);
            binder.bind(Orm.class).to(orm);
            binder.bind(UserRepository.class).to(new UserRepository(db, orm));
            binder.bind(TopicRepository.class).to(new TopicRepository(db, orm));
            binder.bind(CommentRepository.class).to(new CommentRepository(db, orm));
            binder.bind(MentionNotifier.class).to(MentionNotifier.class);
            binder.bind(TopicService.class).to(TopicService.class);
            binder.bind(CommentService.class).to(CommentService.class);
        });
        commentService = container.get(CommentService.class);
        topicService = container.get(TopicService.class);
        db.execute(
            "INSERT INTO bbs_user (id, nickname, username, email_verified, score, exp, level, status, topic_count, comment_count, follow_count, fans_count, forbidden_end_time, create_time, update_time) " +
            "VALUES (1, 'u1', 'u1', TRUE, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0)");
        db.execute(
            "INSERT INTO bbs_user (id, nickname, username, email_verified, score, exp, level, status, topic_count, comment_count, follow_count, fans_count, forbidden_end_time, create_time, update_time) " +
            "VALUES (2, 'u2', 'u2', TRUE, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0)");
        db.execute(
            "INSERT INTO bbs_topic (id, user_id, category_id, type, title, content, status, bounty_score, vote_id, view_count, comment_count, like_count, recommend, recommend_time, sticky, sticky_time, qa_status, solved_at, create_time, last_comment_time) " +
            "VALUES (1, 1, 1, 0, 't', 'c', 1, 0, NULL, 0, 0, 0, FALSE, 0, FALSE, 0, NULL, 0, 0, 0)");
    }

    @org.junit.jupiter.api.BeforeEach
    void resetState() {
        // Tests share the in-memory DB — isolate counters between tests
        db.execute("DELETE FROM bbs_comment");
        db.execute("UPDATE bbs_topic SET comment_count = 0 WHERE id = 1");
        db.execute("UPDATE bbs_user SET comment_count = 0 WHERE id IN (1, 2)");
    }

    @AfterAll
    static void tearDown() throws Exception {
        container.close();
    }

    @Test
    void createIncrementsEntityAndUserCounts() {
        commentService.create(1L, "topic", 1L, "第一条评论", "markdown", null, 0);
        var topic = topicService.findById(1L).orElseThrow();
        assertEquals(1, topic.getCommentCount());

        var cnt = db.query("SELECT comment_count FROM bbs_user WHERE id = 1")
            .one(Row.class).map(r -> r.longValue("comment_count")).orElse(-1L);
        assertEquals(1L, cnt);
    }

    @Test
    void replyIncrementsQuotedCommentCount() {
        var c = commentService.create(2L, "topic", 1L, "被回复的评论", "markdown", null, 0);
        // user 1 replies to user 2's comment
        commentService.create(1L, "topic", 1L, "回复内容", "markdown", null, c.getId());
        var replied = db.query("SELECT comment_count FROM bbs_comment WHERE id = ?", c.getId())
            .one(Row.class).map(r -> r.longValue("comment_count")).orElse(-1L);
        assertEquals(1L, replied);
    }

    @Test
    void deleteDecrementsCountsAndIsScoped() {
        var c = commentService.create(1L, "topic", 1L, "待删除评论", "markdown", null, 0);
        assertThrows(ServiceException.class, () -> commentService.delete(2L, c.getId()));
        commentService.delete(1L, c.getId());

        var topic = topicService.findById(1L).orElseThrow();
        assertEquals(0, topic.getCommentCount());
        var userCnt = db.query("SELECT comment_count FROM bbs_user WHERE id = 1")
            .one(Row.class).map(r -> r.longValue("comment_count")).orElse(-1L);
        assertEquals(0L, userCnt);
    }
}
