package com.nba.user;

import java.util.List;

public record AdminUserDetailDto(
        Long id,
        String username,
        String email,
        List<String> roles,
        String registeredAt,
        String lastLoginAt,
        List<String> linkedProviders
) {
}
