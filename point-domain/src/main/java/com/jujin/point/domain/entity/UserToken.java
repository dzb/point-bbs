package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * User token entity (legacy UUID token, replaced by JWT for web but kept for API compatibility).
 */
@Table("bbs_user_token")
public class UserToken {
    @Id @Generated
    private Long id;
    @Column(length = 32, nullable = false)
    private String token;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private long expiredAt;
    @Column(nullable = false)
    private int status;
    @Column(nullable = false)
    private long createTime;

    public UserToken() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public long getExpiredAt() { return expiredAt; }
    public void setExpiredAt(long expiredAt) { this.expiredAt = expiredAt; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
}
