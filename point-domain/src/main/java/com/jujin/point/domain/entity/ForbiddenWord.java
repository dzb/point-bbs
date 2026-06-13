package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_forbidden_word")
public class ForbiddenWord {
    @Id @Generated
    private Long id;
    @Column(length = 16, nullable = false)
    private String type;     // "word" or "regex"
    @Column(length = 256, nullable = false)
    private String word;
    @Column(length = 256)
    private String remark;
    @Column(nullable = false) long createTime;

    public ForbiddenWord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
