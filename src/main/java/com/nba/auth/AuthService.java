package com.nba.auth;

import com.nba.user.UserShortDto;

public interface AuthService {
    UserShortDto register(RegisterRequest registerRequest);
}
