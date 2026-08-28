package com.nba.user;

import lombok.Builder;

import java.util.List;

@Builder
public record UserFullDto(
        Long id,
        String username,
        String email,
        List<String> roles,
        String registeredAt,
        String lastLogin
) {
}
