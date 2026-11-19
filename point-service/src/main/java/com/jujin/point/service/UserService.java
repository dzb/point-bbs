package com.jujin.point.service;

import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.auth.Passwords;
import com.jujin.point.domain.dto.CurrentUser;
import com.jujin.point.domain.dto.PageRequest;
import com.jujin.point.domain.dto.PageResult;
import com.jujin.point.domain.dto.UserDtos.*;
import com.jujin.point.domain.entity.User;
import com.jujin.point.domain.event.UserForbiddenEvent;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.ioc.event.EventBus;

import java.util.Collection;
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
    private final com.jujin.point.db.repository.ThirdUserRepository thirdUserRepo;
    private final EventBus eventBus;

    public UserService(
        Database db,
        UserRepository userRepo,
        com.jujin.point.db.repository.ThirdUserRepository thirdUserRepo,
        EventBus eventBus
    ) {
        this.db = db;
        this.userRepo = userRepo;
        this.thirdUserRepo = thirdUserRepo;
        this.eventBus = eventBus;
    }

    /**
     * Rejects banned/muted accounts. Called by the auth filter for
     * non-GET requests — muted users keep read access.
     */
    public void ensureUsable(long userId) {
        long[] gate = gateCache.getOrCompute(userId, () -> db
            .query("SELECT status, forbidden_end_time FROM bbs_user WHERE id = $id")
            .bind("id", userId)
            .one(Row.class)
            .map(r -> new long[] { r.asLong("status"), r.asLong("forbidden_end_time") })
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

    /** Admin listing: active users, newest first, with the total count. */
    public PageResult<User> listUsers(PageRequest page) {
        var items = userRepo.findPage(page.page(), page.pageSize());
        var total = userRepo.countAll();
        return new PageResult<>(items, page.page(), page.pageSize(), total);
    }

    /** Display projections for a set of ids — the feed enricher's batch read. */
    public List<User> findSummariesByIds(Collection<Long> ids) {
        return userRepo.findSummariesByIds(ids);
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

    /**
     * Find-or-create the account bound to a third-party identity.
     *
     * <p>The binding and the account are created in one transaction. A unique
     * {@code (open_id, third_type)} violation means a concurrent callback won
     * the race, so the catch resolves the binding they created instead of
     * failing the login.</p>
     *
     * @return the bound user, or empty when this identity cannot be resolved
     */
    public Optional<User> loginWithThirdParty(
        String provider,
        String openId,
        String nickname,
        String avatar
    ) {
        var existing = thirdUserRepo.findByOpenIdAndType(openId, provider);
        if (existing.isPresent()) {
            return userRepo.findById(existing.get().getUserId());
        }

        var now = System.currentTimeMillis();
        var user = new User();
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setEmailVerified(false);
        user.setStatus(1);
        user.setLevel(1);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        try {
            db.transaction(() -> {
                // email_verified is NOT NULL in the schema: a third-party
                // account has no verified local email, but the column still
                // needs an explicit value.
                var created = db.execute(
                    "INSERT INTO bbs_user (nickname, avatar, email_verified, score, exp, level, status, " +
                    "topic_count, comment_count, follow_count, fans_count, forbidden_end_time, " +
                    "create_time, update_time) " +
                    "VALUES (?,?,false,0,0,1,1,0,0,0,0,0,?,?)",
                    nickname, avatar, now, now);
                if (created.hasGeneratedKey()) {
                    user.setId(created.longKey());
                }
                thirdUserRepo.insert(thirdUser(user.getId(), provider, openId, nickname, avatar, now));
            });
            return Optional.of(user);
        } catch (com.jujin.freeway.db.SqlException race) {
            return thirdUserRepo.findByOpenIdAndType(openId, provider)
                .flatMap(binding -> userRepo.findById(binding.getUserId()));
        }
    }

    /** Bind a third-party identity to an existing account; re-binding is a no-op. */
    public void bindThirdParty(
        long userId,
        String provider,
        String openId,
        String nickname,
        String avatar
    ) {
        if (thirdUserRepo.exists(openId, provider)) return;
        thirdUserRepo.insert(thirdUser(userId, provider, openId, nickname, avatar, System.currentTimeMillis()));
    }

    public void unbindThirdParty(long userId, String provider) {
        thirdUserRepo.deleteByUserIdAndType(userId, provider);
    }

    private static com.jujin.point.domain.entity.ThirdUser thirdUser(
        Long userId,
        String provider,
        String openId,
        String nickname,
        String avatar,
        long now
    ) {
        var binding = new com.jujin.point.domain.entity.ThirdUser();
        binding.setUserId(userId);
        binding.setOpenId(openId);
        binding.setThirdType(provider);
        binding.setNickname(nickname);
        binding.setAvatar(avatar);
        binding.setCreateTime(now);
        binding.setUpdateTime(now);
        return binding;
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
        // Defense in depth: the route validates the DTO too, but the service
        // is the single password-mutation owner and must not rely on callers.
        if (newPassword == null || newPassword.length() < 6) {
            throw new ServiceException("新密码长度至少 6 位");
        }
        if (Passwords.verify(newPassword, user.getPassword())) {
            throw new ServiceException("新密码不能与原密码相同");
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
        eventBus.publishEvent(new UserForbiddenEvent(userId, endTime, System.currentTimeMillis()));
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
