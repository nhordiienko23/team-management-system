package com.nba.user;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequestForAdmins(
        @NotBlank(message = "New password is required")
        String newPassword) {
}
