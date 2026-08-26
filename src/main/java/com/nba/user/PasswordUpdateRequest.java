package com.nba.user;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(
        String currentPassword,
        @NotBlank(message = "New password is required")
        String newPassword) {
}
