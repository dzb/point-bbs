package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_level_config")
public class LevelConfig {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private int level;
    @Column(nullable = false)
    private int needExp;
    @Column(length = 64)
    private String title;
    @Column(nullable = false)
    private int status;
    @Column(nullable = false)
    private long createTime;
    @Column(nullable = false)
    private long updateTime;

    public LevelConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getNeedExp() { return needExp; }
    public void setNeedExp(int needExp) { this.needExp = needExp; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
