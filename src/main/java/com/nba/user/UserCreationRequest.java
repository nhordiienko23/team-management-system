package com.nba.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Set;

@Builder
public record UserCreationRequest(
        @NotBlank(message = "Username can't be blank")
        String username,
        @NotBlank(message = "Password can't be blank")
        String password,
        @Email
        @NotBlank(message = "Email can't be blank")
        String email,
        @NotEmpty(message = "User must have at least one role")
        Set<UserRole> roles
) {
}
