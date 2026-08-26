package com.nba.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Username can't be blank")
        String username,
        @NotBlank(message = "Password can't be blank")
        String password,
        @Email
        @NotBlank(message = "Email can't be blank")
        String email) {
}
