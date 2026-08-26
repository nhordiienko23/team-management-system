package com.nba.user;

import lombok.Builder;

@Builder
public record UserDto(
        Long id,
        String username,
        String email) {
}
