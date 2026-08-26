package com.nba.auth;

import com.nba.user.User;
import com.nba.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthMapperImpl implements AuthMapper{
    private final PasswordEncoder passwordEncoder;

    @Override
    public User toUserEntity(RegisterRequest registerRequest) {
        return User.builder()
                .username(registerRequest.username())
                .password(passwordEncoder.encode(registerRequest.password()))
                .roles(Set.of(UserRole.ROLE_USER))
                .email(registerRequest.email())
                .registerAt(LocalDateTime.now())
                .build();
    }
}

