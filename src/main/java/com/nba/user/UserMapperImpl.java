package com.nba.user;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class UserMapperImpl implements UserMapper {
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }


    @Override
    public UserFullDto toUserFullDto(User user) {
        return UserFullDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles().stream()
                        .map(UserRole::name)
                        .toList())
                .registeredAt(user.getRegisterAt().toString())
                .lastLogin((user.getLastLogin() != null)
                        ? user.getLastLogin().toString()
                        : null)
                .build();
    }

    @Override
    public User toUserEntity(UserCreationRequest request) {
        return User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .roles(request.roles())
                .registerAt(LocalDateTime.now())
                .build();
    }


}
