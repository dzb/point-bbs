package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Vote/Poll entity.
 */
@Table("bbs_vote")
public class Vote {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private int type;            // 1=single, 2=multiple
    @Column(length = 128)
    private String title;
    @Column(nullable = false)
    private long expiredAt;
    @Column(nullable = false)
    private Long topicId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private int voteNum;         // max votes per user
    @Column(nullable = false)
    private int optionCount;
    @Column(nullable = false)
    private int voteCount;
    private long createTime;

    public Vote() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getExpiredAt() { return expiredAt; }
    public void setExpiredAt(long expiredAt) { this.expiredAt = expiredAt; }
    public Long getTopicId() { return topicId; }
    public void setTopicId(Long topicId) { this.topicId = topicId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getVoteNum() { return voteNum; }
    public void setVoteNum(int voteNum) { this.voteNum = voteNum; }
    public int getOptionCount() { return optionCount; }
    public void setOptionCount(int optionCount) { this.optionCount = optionCount; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
