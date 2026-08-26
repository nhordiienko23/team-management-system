package com.nba.user;

import jakarta.validation.constraints.Email;


public record UpdateRequest(
        String username,
        @Email
        String email) {
}
