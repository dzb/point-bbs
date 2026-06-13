package com.jujin.point.domain.dto;

/**
 * User-related request DTOs.
 */
public interface UserDtos {

    record CreateUserRequest(
        String nickname,
        String email,
        String phone,
        String username,
        String password,
        String captchaToken,
        String captchaCode,
        String ref
    ) {}

    record UpdateUserRequest(
        String nickname,
        String avatar,
        String gender,
        String description,
        String homePage,
        String backgroundImage
    ) {}

    record SignInRequest(String loginName, String password, String captchaToken, String captchaCode) {}

    record SetPasswordRequest(String oldPassword, String newPassword) {}

    record ResetPasswordRequest(String email, String token, String newPassword) {}

    record SendVerifyEmailRequest(String email) {}

    record SetUsernameRequest(String username) {}

    record SetEmailRequest(String email) {}
}
