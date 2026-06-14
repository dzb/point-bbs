package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * Category node — two-level tree (parentId=0 for top-level).
 */
@Table("bbs_category")
public class Category {
    @Id @Generated
    private Long id;
    @Column(nullable = false)
    private Long parentId;
    @Column(length = 32)
    private String name;
    @Column(length = 16)
    private String type = "normal";  // normal, qa
    @Column(length = 1024)
    private String description;
    @Column(length = 1024)
    private String logo;
    @Column
    private int sortNo;
    @Column(nullable = false)
    private int status;
    @Column(nullable = false)
    private long createTime;

    public Category() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public int getSortNo() { return sortNo; }
    public void setSortNo(int sortNo) { this.sortNo = sortNo; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
