package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

@Table("bbs_third_user")
public class ThirdUser {
    @Id @Generated
    private Long id;
    @Column(nullable = false) Long userId;
    @Column(length = 64, nullable = false) String openId;
    @Column(length = 32, nullable = false) String thirdType;  // weixin, google, github
    @Column(length = 32) String nickname;
    @Column(length = 1024) String avatar;
    @Column(type = "LONGTEXT") String extraData;
    @Column long createTime;
    @Column long updateTime;

    public ThirdUser() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getThirdType() { return thirdType; }
    public void setThirdType(String thirdType) { this.thirdType = thirdType; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
