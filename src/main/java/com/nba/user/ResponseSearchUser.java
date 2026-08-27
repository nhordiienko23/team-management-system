package com.nba.user;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ResponseSearchUser (
        Long id,
        String username,
        String email,
        List<String> roles,
        LocalDateTime registeredAt,
        LocalDateTime lastLogin
){
}
