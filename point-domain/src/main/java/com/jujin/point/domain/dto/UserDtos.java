package com.jujin.point.domain.dto;

import com.jujin.freeway.commons.validation.NotBlank;
import com.jujin.freeway.commons.validation.Size;

/**
 * User-related request DTOs.
 */
public interface UserDtos {

    record CreateUserRequest(
        @NotBlank String nickname,
        @NotBlank String email,
        String phone,
        String username,
        @Size(min = 6) String password,
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

    record SignInRequest(@NotBlank String loginName, @NotBlank String password, String captchaToken, String captchaCode) {}

    record SetPasswordRequest(@NotBlank String oldPassword, @Size(min = 6) String newPassword) {}

    record ResetPasswordRequest(String email, String token, String newPassword) {}

    record SendVerifyEmailRequest(String email) {}

    record SetUsernameRequest(String username) {}

    record SetEmailRequest(String email) {}
}
