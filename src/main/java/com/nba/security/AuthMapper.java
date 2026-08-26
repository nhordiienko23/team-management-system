package com.nba.security;

import com.nba.user.User;

public interface AuthMapper {
    User toUserEntity(RegisterRequest registerRequest);
}
