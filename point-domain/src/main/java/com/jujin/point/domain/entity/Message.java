package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Message/notification entity.
 */
@Table("bbs_message")
public class Message {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private Long fromId;
    @Column(nullable = false)
    private Long userId;
    @Column(length = 1024)
    private String title;
    @Column(type = "TEXT", nullable = false)
    private String content;
    @Column(type = "TEXT")
    private String quoteContent;
    @Column(nullable = false)
    private int type;
    @Column(type = "TEXT")
    private String extraData;
    @Column(nullable = false)
    private int status;          // 0=unread, 1=read
    @Column(nullable = false)
    private long createTime;

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromId() { return fromId; }
    public void setFromId(Long fromId) { this.fromId = fromId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getQuoteContent() { return quoteContent; }
    public void setQuoteContent(String quoteContent) { this.quoteContent = quoteContent; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
