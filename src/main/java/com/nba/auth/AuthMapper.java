package com.nba.auth;

import com.nba.user.User;

public interface AuthMapper {
    User toUserEntity(RegisterRequest registerRequest);
}
