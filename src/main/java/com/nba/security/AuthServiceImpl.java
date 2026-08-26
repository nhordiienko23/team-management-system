package com.nba.security;

import com.nba.core.exception.invalidData.UserInvalidDataException;
import com.nba.user.User;
import com.nba.user.UserDto;
import com.nba.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final AuthMapperImpl authMapper;

    @Override
    public UserDto register(RegisterRequest registerRequest) {
        if (userService.existsByUsername(registerRequest.username()))
            throw new UserInvalidDataException("User with name " + registerRequest.username() + " already exist");
        User newUser = authMapper.toUserEntity(registerRequest);
        return userService.saveToDataBase(newUser);
    }
}
