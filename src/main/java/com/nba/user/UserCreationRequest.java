package com.nba.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.Set;

@Builder
public record UserCreationRequest(
        @NotBlank(message = "Please enter your username.")
        String username,
        @NotBlank(message = "Please enter your password.")
        String password,
        @Email(message = "Please enter a valid email address, like name@example.com.")
        @NotBlank(message = "Please enter your email address.")
        String email,
        @NotEmpty(message = "Please select at least one role.")
        Set<UserRole> roles
) {
}
