package com.nba.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.context.annotation.Bean;

@Builder
public record RegisterRequest(
        @NotBlank(message = "Please enter your username.")
        String username,
        @NotBlank(message = "Please enter your password.")
        String password,
        @Email(message = "Please enter a valid email address, like name@example.com.")
        @NotBlank(message = "Please enter your email address.")
        String email) {
}
