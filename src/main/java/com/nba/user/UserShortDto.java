package com.nba.user;

import lombok.Builder;

@Builder
public record UserShortDto(
        Long id,
        String username,
        String email) {
}
