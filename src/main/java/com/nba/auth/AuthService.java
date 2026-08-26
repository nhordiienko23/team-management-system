package com.nba.auth;

import com.nba.user.UserDto;

public interface AuthService {
    UserDto register(RegisterRequest registerRequest);
}
