package com.jujin.point.service;

import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.User;
import com.jujin.freeway.db.Database;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * User service — registration, login, profile management.
 */
public class UserService {
    private final Database db;
    private final UserRepository userRepo;

    public UserService(Database db, UserRepository userRepo) {
        this.db = db;
        this.userRepo = userRepo;
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
        user.setPassword(hashPassword(req.password()));
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
            if (result.hasKey()) {
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
            throw new ServiceException("用户已被禁言至 " + user.getForbiddenEndTime());
        }

        if (!verifyPassword(password, user.getPassword())) {
            throw new ServiceException("密码错误");
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
        if (!verifyPassword(oldPassword, user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        user.setPassword(hashPassword(newPassword));
        user.setUpdateTime(System.currentTimeMillis());
        userRepo.update(user);
    }

    public void addScore(long userId, int score) {
        db.execute("UPDATE bbs_user SET score = score + ? WHERE id = ?", score, userId);
    }

    public void addExp(long userId, int exp) {
        db.execute("UPDATE bbs_user SET exp = exp + ? WHERE id = ?", exp, userId);
    }

    // --- password helpers ---

    private static String hashPassword(String password) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("hash error", e);
        }
    }

    private static boolean verifyPassword(String password, String stored) {
        if (stored == null || !stored.contains(":")) return false;
        try {
            var parts = stored.split(":");
            byte[] salt = HexFormat.of().parseHex(parts[0]);
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return HexFormat.of().formatHex(hash).equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }
}
