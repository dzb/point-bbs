package com.jujin.point.domain.entity;

import com.jujin.freeway.db.schema.Column;
import com.jujin.freeway.db.schema.Generated;
import com.jujin.freeway.db.schema.Id;
import com.jujin.freeway.db.schema.Table;

/**
 * User entity — mutable bean for partial updates.
 */
@Table("bbs_user")
public class User {
    @Id @Generated
    private Long id;
    @Column(length = 16)
    private String phone;
    @Column(length = 32)
    private String username;
    @Column(length = 128)
    private String email;
    @Column(nullable = false)
    private boolean emailVerified;
    @Column(length = 16)
    private String nickname;
    @Column(type = "TEXT")
    private String avatar;
    @Column(length = 16)
    private String gender;
    @Column
    private Long birthday;  // epoch millis, nullable
    @Column(type = "TEXT")
    private String backgroundImage;
    @Column(length = 512)
    private String password;
    @Column(length = 1024)
    private String homePage;
    @Column(type = "TEXT")
    private String description;
    @Column(nullable = false)
    private int score;
    @Column(nullable = false)
    private int exp;
    @Column(nullable = false)
    private int level = 1;
    @Column(nullable = false)
    private int status;
    @Column(nullable = false)
    private int topicCount;
    @Column(nullable = false)
    private int commentCount;
    @Column(nullable = false)
    private int followCount;
    @Column(nullable = false)
    private int fansCount;
    @Column(type = "TEXT")
    private String roles;
    @Column(nullable = false)
    private long forbiddenEndTime;
    @Column(nullable = false)
    private long createTime;
    @Column
    private long updateTime;

    public User() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Long getBirthday() { return birthday; }
    public void setBirthday(Long birthday) { this.birthday = birthday; }
    public String getBackgroundImage() { return backgroundImage; }
    public void setBackgroundImage(String backgroundImage) { this.backgroundImage = backgroundImage; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getHomePage() { return homePage; }
    public void setHomePage(String homePage) { this.homePage = homePage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getTopicCount() { return topicCount; }
    public void setTopicCount(int topicCount) { this.topicCount = topicCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getFollowCount() { return followCount; }
    public void setFollowCount(int followCount) { this.followCount = followCount; }
    public int getFansCount() { return fansCount; }
    public void setFansCount(int fansCount) { this.fansCount = fansCount; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public long getForbiddenEndTime() { return forbiddenEndTime; }
    public void setForbiddenEndTime(long forbiddenEndTime) { this.forbiddenEndTime = forbiddenEndTime; }
    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }
    public long getUpdateTime() { return updateTime; }
    public void setUpdateTime(long updateTime) { this.updateTime = updateTime; }
}
