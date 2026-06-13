package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_task_config")
public class TaskConfig {
    @Id @Generated
    private Long id;
    @Column(length = 32, nullable = false)
    private String groupName;
    @Column(length = 64, nullable = false)
    private String eventType;
    @Column(length = 64, nullable = false)
    private String title;
    @Column(length = 512, nullable = false)
    private String description;
    @Column int score;
    @Column int exp;
    @Column Long badgeId;
    @Column(nullable = false) int period;
    @Column(nullable = false) int maxFinishCount = 1;
    @Column(nullable = false) int eventCount = 1;
    @Column(length = 32) String btnName;
    @Column(length = 1024) String actionUrl;
    @Column int sortNo;
    @Column long startTime;
    @Column long endTime;
    @Column(nullable = false) int status;
    @Column(nullable = false) long createTime;
    @Column(nullable = false) long updateTime;

    public TaskConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public Long getBadgeId() { return badgeId; }
    public void setBadgeId(Long badgeId) { this.badgeId = badgeId; }
    public int getPeriod() { return period; }
    public void setPeriod(int period) { this.period = period; }
    public int getMaxFinishCount() { return maxFinishCount; }
    public void setMaxFinishCount(int maxFinishCount) { this.maxFinishCount = maxFinishCount; }
    public int getEventCount() { return eventCount; }
    public void setEventCount(int eventCount) { this.eventCount = eventCount; }
    public String getBtnName() { return btnName; }
    public void setBtnName(String btnName) { this.btnName = btnName; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
