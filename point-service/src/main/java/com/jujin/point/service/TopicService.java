package com.jujin.point.service;

import com.jujin.point.db.repository.TopicRepository;
import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.PageResult;
import com.jujin.point.domain.dto.TopicDtos.*;
import com.jujin.point.domain.entity.Topic;
import com.jujin.point.domain.entity.TopicTag;
import com.jujin.point.domain.event.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.ioc.EventBus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Topic service — CRUD, listing, search.
 */
public class TopicService {
    private final Database db;
    private final TopicRepository topicRepo;
    private final UserRepository userRepo;
    private final EventBus eventBus;

    public TopicService(Database db, TopicRepository topicRepo, UserRepository userRepo, EventBus eventBus) {
        this.db = db;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
        this.eventBus = eventBus;
    }

    public Optional<Topic> findById(long id) {
        return topicRepo.findById(id);
    }

    public Topic create(long userId, CreateTopicRequest req) {
        var now = System.currentTimeMillis();
        var topic = new Topic();
        topic.setUserId(userId);
        topic.setType(req.type());
        topic.setCategoryId(req.categoryId() != null ? req.categoryId() : 0L);
        topic.setTitle(req.title());
        topic.setContent(req.content());
        topic.setContentType(req.contentType() != null ? req.contentType() : "markdown");
        topic.setImageList(req.imageList());
        topic.setHideContent(req.hideContent());
        topic.setStatus(1);
        topic.setCreateTime(now);
        topic.setLastCommentTime(now);
        topic.setBountyScore(req.bountyScore());

        db.transaction(() -> {
            topicRepo.insert(topic);
            db.execute("UPDATE bbs_user SET topic_count = topic_count + 1 WHERE id = ?", userId);
            eventBus.publish(new TopicCreatedEvent(userId, topic.getId(), req.type(), now));
            // Notify @mentioned users
            var mentioned = MentionParser.extractMentions(req.content());
            var notified = new HashSet<Long>();
            for (String username : mentioned) {
                userRepo.findByUsername(username).ifPresent(u -> {
                    if (u.getId() != userId && notified.add(u.getId())) {
                        eventBus.publish(new UserMentionedEvent(userId, u.getId(),
                            "topic", topic.getId(), Strings.truncate(req.content(), 100), now));
                    }
                });
            }
        });

        return topic;
    }

    public Topic update(long userId, long topicId, UpdateTopicRequest req) {
        var topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ServiceException("帖子不存在"));
        if (topic.getUserId() != userId) {
            throw new ServiceException("无权修改");
        }
        if (req.title() != null) topic.setTitle(req.title());
        if (req.content() != null) topic.setContent(req.content());
        if (req.contentType() != null) topic.setContentType(req.contentType());
        if (req.categoryId() != null) topic.setCategoryId(req.categoryId());
        if (req.hideContent() != null) topic.setHideContent(req.hideContent());

        topicRepo.update(topic);
        eventBus.publish(new TopicUpdatedEvent(userId, topicId, System.currentTimeMillis()));
        return topic;
    }

    public void delete(long userId, long topicId) {
        var topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ServiceException("帖子不存在"));
        if (topic.getUserId() != userId) {
            throw new ServiceException("无权删除");
        }
        topic.setStatus(0); // soft delete
        topicRepo.update(topic);
        eventBus.publish(new TopicDeletedEvent(userId, topicId, userId, System.currentTimeMillis()));
    }

    public void incrViewCount(long topicId) {
        topicRepo.incrViewCount(topicId);
    }

    public PageResult<Topic> getRecentTopics(PageRequest page) {
        var items = topicRepo.findRecentTopics(page.page(), page.pageSize());
        var total = topicRepo.count();
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }

    public PageResult<Topic> getUserTopics(long userId, PageRequest page) {
        var items = topicRepo.findByUserId(userId, page.page(), page.pageSize());
        var total = topicRepo.countByUserId(userId);
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }

    public PageResult<Topic> getTopicsByCategory(long categoryId, PageRequest page) {
        var items = topicRepo.findByCategoryId(categoryId, page.page(), page.pageSize());
        var total = topicRepo.countByCategoryId(categoryId);
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }

    public List<Topic> getRecommended(int limit) {
        return topicRepo.findRecommended(limit);
    }

    public List<Topic> getSticky() {
        return topicRepo.findSticky();
    }

    public PageResult<Topic> search(String keyword, PageRequest page) {
        var items = topicRepo.searchByTitle(keyword, page.page(), page.pageSize());
        var total = topicRepo.countByTitleSearch(keyword);
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }

    public void recommend(long topicId, boolean recommend) {
        var topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ServiceException("帖子不存在"));
        topic.setRecommend(recommend);
        topic.setRecommendTime(recommend ? System.currentTimeMillis() : 0);
        topicRepo.update(topic);
        eventBus.publish(new TopicRecommendedEvent(topicId, recommend, System.currentTimeMillis()));
    }

    public void sticky(long topicId, boolean sticky) {
        var topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ServiceException("帖子不存在"));
        topic.setSticky(sticky);
        topic.setStickyTime(sticky ? System.currentTimeMillis() : 0);
        topicRepo.update(topic);
    }

    public void acceptAnswer(long topicId, long commentId) {
        var topic = topicRepo.findById(topicId)
            .orElseThrow(() -> new ServiceException("帖子不存在"));
        topic.setQaStatus("solved");
        topic.setAcceptedCommentId(commentId);
        topic.setSolvedAt(System.currentTimeMillis());
        topicRepo.update(topic);
        eventBus.publish(new QaAnswerAcceptedEvent(topic.getUserId(), topicId, commentId, System.currentTimeMillis()));
    }
}
