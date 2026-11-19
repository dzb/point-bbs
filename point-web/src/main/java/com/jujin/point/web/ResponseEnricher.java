package com.jujin.point.web;

import com.jujin.point.domain.entity.*;
import com.jujin.point.service.CommentService;
import com.jujin.point.service.Strings;
import com.jujin.point.service.TagService;
import com.jujin.point.service.UserService;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enriches entities with related user info for frontend display.
 *
 * <p>Bound as a singleton in {@link WebModule}; the batch reads live in the
 * service layer, so this class only shapes responses. It used to run its own
 * SQL, which put table knowledge in the web tier and kept the queries out of
 * reach of service tests.</p>
 */
public class ResponseEnricher {

    private final UserService userSvc;
    private final CommentService commentSvc;
    private final TagService tagSvc;

    public ResponseEnricher(UserService userSvc, CommentService commentSvc, TagService tagSvc) {
        this.userSvc = userSvc;
        this.commentSvc = commentSvc;
        this.tagSvc = tagSvc;
    }

    // -- single-item (for detail endpoints) --

    public Map<String, Object> enrichTopic(Topic t) {
        return buildTopic(t, queryUser(t.getUserId()));
    }

    public Map<String, Object> enrichComment(Comment c) {
        var quoted = c.getQuoteId() != null && c.getQuoteId() > 0
            ? queryQuotePreview(c.getQuoteId())
            : null;
        return buildComment(c, queryUser(c.getUserId()), quoted);
    }

    public Map<String, Object> enrichArticle(Article a) {
        return buildArticle(a, queryUser(a.getUserId()), queryTags(a.getId()));
    }

    // -- batch (for list endpoints) --

    public List<Map<String, Object>> enrichTopics(List<Topic> topics) {
        var users = batchQueryUsers(
            topics.stream().map(Topic::getUserId).collect(Collectors.toSet())
        );
        return topics
            .stream()
            .map(t -> buildTopic(t, users.get(t.getUserId())))
            .toList();
    }

    public List<Map<String, Object>> enrichComments(List<Comment> comments) {
        var users = batchQueryUsers(
            comments
                .stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet())
        );
        var quoteIds = comments
            .stream()
            .map(Comment::getQuoteId)
            .filter(q -> q != null && q > 0)
            .collect(Collectors.toSet());
        var previews = batchQueryQuotePreviews(quoteIds);
        return comments
            .stream()
            .map(c ->
                buildComment(
                    c,
                    users.get(c.getUserId()),
                    c.getQuoteId() != null ? previews.get(c.getQuoteId()) : null
                )
            )
            .toList();
    }

    public List<Map<String, Object>> enrichArticles(List<Article> articles) {
        var userIds = articles
            .stream()
            .map(Article::getUserId)
            .collect(Collectors.toSet());
        var users = batchQueryUsers(userIds);
        var articleIds = articles.stream().map(Article::getId).toList();
        var tagsMap = batchQueryTags(articleIds);
        return articles
            .stream()
            .map(a ->
                buildArticle(
                    a,
                    users.get(a.getUserId()),
                    tagsMap.getOrDefault(a.getId(), List.of())
                )
            )
            .toList();
    }

    // -- builders --

    private Map<String, Object> buildTopic(Topic t, Map<String, Object> user) {
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

    private Map<String, Object> buildComment(
        Comment c,
        Map<String, Object> user,
        String quoteContent
    ) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("entityType", c.getEntityType());
        m.put("entityId", c.getEntityId());
        m.put("content", c.getContent());
        m.put("contentType", c.getContentType());
        m.put("quoteId", c.getQuoteId());
        m.put("quoteContent", quoteContent);
        m.put("likeCount", c.getLikeCount());
        m.put("commentCount", c.getCommentCount());
        m.put("status", c.getStatus());
        m.put("createTime", c.getCreateTime());
        m.put("imageList", c.getImageList());
        m.put("user", user);
        return m;
    }

    private Map<String, Object> buildArticle(
        Article a,
        Map<String, Object> user,
        List<String> tags
    ) {
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

    private Map<Long, Map<String, Object>> batchQueryUsers(Set<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (var user : userSvc.findSummariesByIds(userIds)) {
            result.put(user.getId(), userSummary(user));
        }
        return result;
    }

    private Map<Long, List<String>> batchQueryTags(List<Long> articleIds) {
        return tagSvc.findNamesByArticleIds(articleIds);
    }

    private Map<String, Object> queryUser(long userId) {
        return userSvc.findById(userId).map(ResponseEnricher::userSummary).orElseGet(LinkedHashMap::new);
    }

    /** Preview of a single quoted comment (content truncated). */
    private String queryQuotePreview(long quoteId) {
        return commentSvc.findContentsByIds(List.of(quoteId))
            .stream()
            .findFirst()
            .map(c -> Strings.truncate(c.getContent(), 120))
            .orElse(null);
    }

    /** Batch previews for quoted comments — one IN query, no N+1. */
    private Map<Long, String> batchQueryQuotePreviews(Set<Long> quoteIds) {
        if (quoteIds.isEmpty()) return Map.of();
        Map<Long, String> result = new HashMap<>();
        for (var c : commentSvc.findContentsByIds(quoteIds)) {
            result.put(c.getId(), Strings.truncate(c.getContent(), 120));
        }
        return result;
    }

    private List<String> queryTags(long articleId) {
        return tagSvc.findNamesByArticleIds(List.of(articleId)).getOrDefault(articleId, List.of());
    }

    /** Wire shape of the embedded author — kept here because it is presentation. */
    private static Map<String, Object> userSummary(User user) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", user.getId());
        m.put("nickname", user.getNickname());
        m.put("username", user.getUsername());
        m.put("avatar", user.getAvatar());
        return m;
    }
}
