package com.nba.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
public record UserUpdateRequest(
        String username,
        @Size(min = 1, message = "Please enter your email address.")
        @Email(message = "Please enter a valid email address, like name@example.com.")
        String email) {
}
