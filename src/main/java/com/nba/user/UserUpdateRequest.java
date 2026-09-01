package com.nba.user;

import jakarta.validation.constraints.Email;
import lombok.Builder;


@Builder
public record UserUpdateRequest(
        String username,
        @Email
        String email) {
}
