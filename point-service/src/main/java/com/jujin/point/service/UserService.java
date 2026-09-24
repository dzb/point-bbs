package com.jujin.point.service;

import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.auth.Passwords;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.User;
import com.jujin.point.domain.event.UserForbiddenEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.ioc.event.EventBus;

import java.util.List;
import java.util.Optional;

/**
 * User service — registration, login, profile management.
 */
public class UserService {
    // Per-request account gate (status + mute deadline), cached 60s so the
    // auth filter does not hit the DB on every request. setForbiddenEndTime
    // invalidates the entry, making a mute bite on the next request.
    private final com.jujin.point.cache.SimpleCache<Long, long[]> gateCache =
        new com.jujin.point.cache.SimpleCache<>(60_000, 10_000);

    private final Database db;
    private final UserRepository userRepo;
    private final EventBus eventBus;

    public UserService(Database db, UserRepository userRepo, EventBus eventBus) {
        this.db = db;
        this.userRepo = userRepo;
        this.eventBus = eventBus;
    }

    /**
     * Rejects banned/muted accounts. Called by the auth filter for
     * non-GET requests — muted users keep read access.
     */
    public void ensureUsable(long userId) {
        long[] gate = gateCache.getOrCompute(userId, () -> db
            .query("SELECT status, forbidden_end_time FROM bbs_user WHERE id = $id")
            .param("id", userId)
            .one(Row.class)
            .map(r -> new long[] { r.longValue("status"), r.longValue("forbidden_end_time") })
            .orElse(new long[] { 0, 0 }));
        if (gate[0] == 0) throw new ServiceException("账号已被禁用");
        if (gate[1] > System.currentTimeMillis()) {
            throw new ServiceException("账号已被禁言至 " + formatTime(gate[1]));
        }
    }

    public Optional<User> findById(long id) {
        return userRepo.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public List<User> searchByPrefix(String prefix, int limit) {
        return userRepo.searchByPrefix(prefix, limit);
    }

    public User signUp(CreateUserRequest req) {
        // Check uniqueness
        if (req.email() != null && userRepo.findByEmail(req.email()).isPresent()) {
            throw new ServiceException("邮箱已被注册");
        }
        if (req.username() != null && userRepo.findByUsername(req.username()).isPresent()) {
            throw new ServiceException("用户名已被使用");
        }

        var user = new User();
        var now = System.currentTimeMillis();
        user.setEmail(req.email());
        user.setUsername(req.username() != null ? req.username() : req.email());
        user.setNickname(req.nickname() != null ? req.nickname() : ("用户" + Long.toHexString(now).substring(4)));
        user.setPassword(Passwords.hash(req.password()));
        user.setEmailVerified(false);
        user.setScore(0);
        user.setExp(0);
        user.setLevel(1);
        user.setStatus(1); // active
        user.setCreateTime(now);
        user.setUpdateTime(now);

        db.transaction(() -> {
            var result = db.execute(
                "INSERT INTO bbs_user (nickname, email, username, password, email_verified, score, exp, level, status, " +
                "topic_count, comment_count, follow_count, fans_count, forbidden_end_time, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, 0, 0, 1, 1, 0, 0, 0, 0, 0, ?, ?)",
                user.getNickname(), user.getEmail(), user.getUsername(), user.getPassword(),
                user.isEmailVerified(), now, now);
            if (result.hasGeneratedKey()) {
                user.setId(result.longKey());
            }
        });
        return user;
    }

    public User signIn(String loginName, String password) {
        var user = userRepo.findByEmail(loginName)
            .or(() -> userRepo.findByUsername(loginName))
            .or(() -> userRepo.findByPhone(loginName))
            .orElseThrow(() -> new ServiceException("用户不存在"));

        if (user.getStatus() == 0) {
            throw new ServiceException("用户已被禁用");
        }

        if (user.getForbiddenEndTime() > System.currentTimeMillis()) {
            throw new ServiceException("用户已被禁言至 " + formatTime(user.getForbiddenEndTime()));
        }

        if (!Passwords.verify(password, user.getPassword())) {
            throw new ServiceException("密码错误");
        }

        // Transparently upgrade legacy SHA-256 hashes to PBKDF2 on login
        if (!Passwords.isCurrentFormat(user.getPassword())) {
            user.setPassword(Passwords.hash(password));
            user.setUpdateTime(System.currentTimeMillis());
            userRepo.update(user);
        }

        return user;
    }

    public void updateUser(long userId, UpdateUserRequest req) {
        var user = userRepo.findById(userId)
            .orElseThrow(() -> new ServiceException("用户不存在"));

        if (req.nickname() != null) user.setNickname(req.nickname());
        if (req.avatar() != null) user.setAvatar(req.avatar());
        if (req.gender() != null) user.setGender(req.gender());
        if (req.description() != null) user.setDescription(req.description());
        if (req.homePage() != null) user.setHomePage(req.homePage());
        if (req.backgroundImage() != null) user.setBackgroundImage(req.backgroundImage());
        user.setUpdateTime(System.currentTimeMillis());

        userRepo.update(user);
    }

    public void setPassword(long userId, String oldPassword, String newPassword) {
        var user = userRepo.findById(userId)
            .orElseThrow(() -> new ServiceException("用户不存在"));
        if (!Passwords.verify(oldPassword, user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        user.setPassword(Passwords.hash(newPassword));
        user.setUpdateTime(System.currentTimeMillis());
        userRepo.update(user);
    }

    /** Set the mute/forbid deadline (epoch millis; 0 lifts the forbid). */
    public void setForbiddenEndTime(long userId, long endTime) {
        db.execute(
            "UPDATE bbs_user SET forbidden_end_time = ?, update_time = ? WHERE id = ?",
            endTime, System.currentTimeMillis(), userId
        );
        // Mute/unmute must bite immediately and notify the user
        gateCache.invalidate(userId);
        eventBus.publish(new UserForbiddenEvent(userId, endTime, System.currentTimeMillis()));
    }

    private static String formatTime(long epochMillis) {
        return Strings.formatTime(epochMillis);
    }

    public void addScore(long userId, int score) {
        db.execute("UPDATE bbs_user SET score = score + ? WHERE id = ?", score, userId);
    }

    public void addExp(long userId, int exp) {
        db.execute("UPDATE bbs_user SET exp = exp + ? WHERE id = ?", exp, userId);
    }

}
