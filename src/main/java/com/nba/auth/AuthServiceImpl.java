package com.nba.auth;

import com.nba.user.User;
import com.nba.user.UserService;
import com.nba.user.UserShortDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final AuthMapperImpl authMapper;

    @Override
    @Transactional
    public UserShortDto register(RegisterRequest registerRequest) {
        userService.validateUsernameIsFree(registerRequest.username());
        userService.validateEmailIsFree(registerRequest.email());
        User newUser = authMapper.toUserEntity(registerRequest);
        return userService.saveToDataBaseAndReturnDto(newUser);
    }
}
