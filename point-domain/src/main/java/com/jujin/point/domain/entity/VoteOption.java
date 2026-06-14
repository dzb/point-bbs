package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Vote option entity.
 */
@Table("bbs_vote_option")
public class VoteOption {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private Long voteId;
    @Column(length = 256)
    private String content;
    @Column(nullable = false)
    private int sortNo;
    @Column(nullable = false)
    private int voteCount;
    @Column(nullable = false)
    private long createTime;

    public VoteOption() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVoteId() { return voteId; }
    public void setVoteId(Long voteId) { this.voteId = voteId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
