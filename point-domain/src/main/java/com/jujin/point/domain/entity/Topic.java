package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Topic/Post entity — supports discussion, tweet, and QA types.
 */
@Table("bbs_topic")
public class Topic {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private int type;           // 0=discussion, 1=tweet, 2=QA
    @Column(nullable = false)
    private Long categoryId;
    @Column(length = 16)
    private String qaStatus;    // unsolved, solved
    @Column
    private Long acceptedCommentId;
    @Column
    private long solvedAt;
    @Column
    private int bountyScore;
    @Column(nullable = false)
    private Long userId;
    @Column(length = 128)
    private String title;
    @Column(length = 32)
    private String contentType = "markdown";
    @Column(type = "LONGTEXT")
    private String content;
    @Column(type = "LONGTEXT")
    private String imageList;
    @Column(type = "LONGTEXT")
    private String hideContent;
    @Column
    private Long voteId;
    @Column(nullable = false)
    private boolean recommend;
    @Column
    private long recommendTime;
    @Column(nullable = false)
    private boolean sticky;
    @Column
    private long stickyTime;
    @Column(nullable = false)
    private long viewCount;
    @Column(nullable = false)
    private long commentCount;
    @Column(nullable = false)
    private long likeCount;
    @Column(nullable = false)
    private int status;
    @Column
    private long lastCommentTime;
    @Column
    private Long lastCommentUserId;
    @Column(length = 1024)
    private String userAgent;
    @Column(length = 128)
    private String ip;
    @Column(length = 64)
    private String ipLocation;
    @Column
    private long createTime;
    @Column(type = "TEXT")
    private String extraData;

    public Topic() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getQaStatus() { return qaStatus; }
    public void setQaStatus(String qaStatus) { this.qaStatus = qaStatus; }
    public Long getAcceptedCommentId() { return acceptedCommentId; }
    public void setAcceptedCommentId(Long acceptedCommentId) { this.acceptedCommentId = acceptedCommentId; }
    public long getSolvedAt() { return solvedAt; }
    public void setSolvedAt(long solvedAt) { this.solvedAt = solvedAt; }
    public int getBountyScore() { return bountyScore; }
    public void setBountyScore(int bountyScore) { this.bountyScore = bountyScore; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getImageList() { return imageList; }
    public void setImageList(String imageList) { this.imageList = imageList; }
    public String getHideContent() { return hideContent; }
    public void setHideContent(String hideContent) { this.hideContent = hideContent; }
    public Long getVoteId() { return voteId; }
    public void setVoteId(Long voteId) { this.voteId = voteId; }
    public boolean isRecommend() { return recommend; }
    public void setRecommend(boolean recommend) { this.recommend = recommend; }
    public long getRecommendTime() { return recommendTime; }
    public void setRecommendTime(long recommendTime) { this.recommendTime = recommendTime; }
    public boolean isSticky() { return sticky; }
    public void setSticky(boolean sticky) { this.sticky = sticky; }
    public long getStickyTime() { return stickyTime; }
    public void setStickyTime(long stickyTime) { this.stickyTime = stickyTime; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getLastCommentTime() { return lastCommentTime; }
    public void setLastCommentTime(long lastCommentTime) { this.lastCommentTime = lastCommentTime; }
    public Long getLastCommentUserId() { return lastCommentUserId; }
    public void setLastCommentUserId(Long lastCommentUserId) { this.lastCommentUserId = lastCommentUserId; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getIpLocation() { return ipLocation; }
    public void setIpLocation(String ipLocation) { this.ipLocation = ipLocation; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
}
