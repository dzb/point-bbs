package com.jujin.point.service;

import static org.junit.jupiter.api.Assertions.*;

import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.DatabaseBuilder;
import com.jujin.freeway.db.Orm;
import com.jujin.freeway.db.PoolConfig;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.Freeway;
import com.jujin.point.db.repository.UserRepository;
import com.jujin.point.domain.dto.UserDtos.CreateUserRequest;
import com.jujin.point.domain.entity.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private static Container container;
    private static UserService userService;
    private static UserRepository userRepo;

    @BeforeAll
    static void setUp() {
        var config = PoolConfig.defaults(
            "jdbc:h2:mem:point_user_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        var db = DatabaseBuilder.from(config).build();
        var orm = Orm.of(db);
        Schema.ensure(db, new Class<?>[] { User.class });

        container = Freeway.create(binder -> {
            binder.bind(Database.class).to(container -> db);
            binder.bind(Orm.class).to(container -> orm);
            binder.bind(UserRepository.class).to(container -> new UserRepository(db, orm));
            binder.bind(UserService.class).to(UserService.class);
        });
        userService = container.get(UserService.class);
        userRepo = container.get(UserRepository.class);
    }

    @AfterAll
    static void tearDown() throws Exception {
        container.close();
    }

    private static CreateUserRequest req(String username, String email, String password) {
        return new CreateUserRequest("昵称-" + username, email, null, username, password, null, null, null);
    }

    @Test
    void signUpStoresPbkdf2Hash() {
        var user = userService.signUp(req("hashuser1", "hashuser1@test.com", "pw123456"));
        assertNotNull(user.getId());
        assertTrue(user.getPassword().startsWith("pbkdf2$"),
            "password must use PBKDF2 format, was: " + user.getPassword());
    }

    @Test
    void signUpRejectsDuplicateUsernameAndEmail() {
        userService.signUp(req("dupuser", "dupuser@test.com", "pw123456"));
        assertThrows(ServiceException.class, () ->
            userService.signUp(req("dupuser", "other@test.com", "pw123456")));
        assertThrows(ServiceException.class, () ->
            userService.signUp(req("otheruser", "dupuser@test.com", "pw123456")));
    }

    @Test
    void signInVerifiesPasswordAndUpgradesLegacyHash() {
        var user = userService.signUp(req("legacyuser", "legacyuser@test.com", "pw123456"));

        // Simulate a legacy SHA-256 hash (salt:hash format) in the DB — the
        // format used before the PBKDF2 upgrade.
        String legacyHash;
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] salt = new byte[16];
            for (int i = 0; i < salt.length; i++) salt[i] = (byte) i;
            md.update(salt);
            byte[] hash = md.digest("pw123456".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            legacyHash = java.util.HexFormat.of().formatHex(salt) + ":"
                + java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // UPDATE via the raw Database (BaseRepository.execute is protected)
        container.get(Database.class).execute(
            "UPDATE bbs_user SET password = ? WHERE id = ?",
            legacyHash,
            user.getId()
        );

        var signedIn = userService.signIn("legacyuser", "pw123456");
        assertEquals(user.getId(), signedIn.getId());
        // Legacy hash must have been transparently upgraded on login
        var reloaded = userRepo.findById(user.getId()).orElseThrow();
        assertTrue(reloaded.getPassword().startsWith("pbkdf2$"),
            "legacy hash should be upgraded to PBKDF2 on login");

        assertThrows(ServiceException.class, () -> userService.signIn("legacyuser", "wrong-password"));
    }

    @Test
    void signInRejectsDisabledUser() {
        var user = userService.signUp(req("disableduser", "disableduser@test.com", "pw123456"));
        container.get(Database.class).execute("UPDATE bbs_user SET status = 0 WHERE id = ?", user.getId());
        assertThrows(ServiceException.class, () -> userService.signIn("disableduser", "pw123456"));
    }

    @Test
    void signInRejectsForbiddenUser() {
        var user = userService.signUp(req("forbiddenuser", "forbiddenuser@test.com", "pw123456"));
        userService.setForbiddenEndTime(user.getId(), System.currentTimeMillis() + 3600_000);
        assertThrows(ServiceException.class, () -> userService.signIn("forbiddenuser", "pw123456"));

        userService.setForbiddenEndTime(user.getId(), 0);
        assertDoesNotThrow(() -> userService.signIn("forbiddenuser", "pw123456"));
    }

    @Test
    void updateUserOnlyPersistsProvidedFields() {
        var user = userService.signUp(req("partialuser", "partialuser@test.com", "pw123456"));
        userService.updateUser(user.getId(),
            new com.jujin.point.domain.dto.UserDtos.UpdateUserRequest(
                "新昵称", null, null, null, null, null));
        var reloaded = userRepo.findById(user.getId()).orElseThrow();
        assertEquals("新昵称", reloaded.getNickname());
        assertEquals("partialuser@test.com", reloaded.getEmail(), "unset fields must survive");
    }
}
