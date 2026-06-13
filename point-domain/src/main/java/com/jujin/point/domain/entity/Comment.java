package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Comment entity — top-level comments and replies (nested via entityType).
 */
@Table("bbs_comment")
public class Comment {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private Long userId;
    @Column(length = 64, nullable = false)
    private String entityType;   // "topic" or "comment"
    @Column(nullable = false)
    private Long entityId;
    @Column(type = "TEXT", nullable = false)
    private String content;
    @Column(type = "LONGTEXT")
    private String imageList;
    @Column(length = 32)
    private String contentType = "markdown";
    @Column
    private Long quoteId;
    @Column(nullable = false)
    private long likeCount;
    @Column(nullable = false)
    private long commentCount;
    @Column(length = 1024)
    private String userAgent;
    @Column(length = 128)
    private String ip;
    @Column(length = 64)
    private String ipLocation;
    @Column(nullable = false)
    private int status;
    @Column
    private long createTime;

    public Comment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageList() { return imageList; }
    public void setImageList(String imageList) { this.imageList = imageList; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getIpLocation() { return ipLocation; }
    public void setIpLocation(String ipLocation) { this.ipLocation = ipLocation; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
