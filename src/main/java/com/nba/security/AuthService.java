package com.nba.security;

import com.nba.user.UserDto;

public interface AuthService {
    UserDto register(RegisterRequest registerRequest);
}
