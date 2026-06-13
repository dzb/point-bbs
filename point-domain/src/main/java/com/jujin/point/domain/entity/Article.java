package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Article entity — blog-style long-form content.
 */
@Table("bbs_article")
public class Article {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(length = 128)
    private String title;
    @Column(type = "TEXT")
    private String summary;
    @Column(type = "LONGTEXT")
    private String content;
    @Column(length = 32)
    private String contentType = "markdown";
    @Column(type = "TEXT")
    private String cover;
    @Column(nullable = false)
    private int status;
    @Column(type = "TEXT")
    private String sourceUrl;
    @Column(nullable = false)
    private long viewCount;
    @Column(nullable = false)
    private long commentCount;
    @Column(nullable = false)
    private long likeCount;
    private long createTime;
    private long updateTime;

    public Article() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
