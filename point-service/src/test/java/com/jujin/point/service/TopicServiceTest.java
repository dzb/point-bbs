package com.jujin.point.service;

import static org.junit.jupiter.api.Assertions.*;

import com.jujin.freeway.db.*;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.Freeway;
import com.jujin.freeway.ioc.Module;
import com.jujin.point.db.repository.TopicRepository;
import com.jujin.point.domain.dto.TopicDtos.CreateTopicRequest;
import com.jujin.point.domain.entity.Comment;
import com.jujin.point.domain.entity.Favorite;
import com.jujin.point.domain.entity.Message;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.entity.User;
import com.jujin.point.domain.entity.UserFollow;
import com.jujin.point.domain.entity.UserLike;

import org.junit.jupiter.api.*;

import java.util.List;

class TopicServiceTest {

    private static Container container;
    private static TopicService topicService;

    @BeforeAll
    static void setUp() {
        var config = PoolConfig.defaults(
            "jdbc:h2:mem:point_test;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var db = DatabaseBuilder.from(config).build();
        var orm = Orm.of(db);
        Schema.ensure(db, ALL_ENTITIES);

        container = Freeway.create(binder -> {
            binder.bind(Database.class).to(db);
            binder.bind(Orm.class).to(orm);
            binder.bind(TopicRepository.class).to(new TopicRepository(db, orm));
            binder.bind(TopicService.class).to(TopicService.class);
        });
        topicService = container.get(TopicService.class);
    }

    @AfterAll
    static void tearDown() throws Exception {
        container.close();
    }

    @Test
    void shouldCreateAndFindTopic() {
        var req = new CreateTopicRequest(0, null, "测试标题", "测试内容", "markdown",
            List.of(), null, null, null, 0);
        var topic = topicService.create(1L, req);
        assertNotNull(topic.getId());
        assertEquals("测试标题", topic.getTitle());

        var found = topicService.findById(topic.getId());
        assertTrue(found.isPresent());
        assertEquals("测试内容", found.get().getContent());
    }

    @Test
    void shouldReturnEmptyForMissingTopic() {
        assertTrue(topicService.findById(99999L).isEmpty());
    }

    @Test
    void shouldSoftDeleteTopic() {
        var req = new CreateTopicRequest(1, null, "", "删除测试", "markdown",
            List.of(), null, null, null, 0);
        var topic = topicService.create(1L, req);
        assertTrue(topicService.findById(topic.getId()).isPresent());

        topicService.delete(1L, topic.getId());
        // After soft-delete (status=0), findById returns empty
        var deleted = topicService.findById(topic.getId());
        assertTrue(deleted.isEmpty() || deleted.get().getStatus() == 0);
    }

    static final Class<?>[] ALL_ENTITIES = {
        Topic.class, User.class, Comment.class,
        UserLike.class, Favorite.class, UserFollow.class, Message.class
    };
}
