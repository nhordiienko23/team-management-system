package com.nba.auth;

import com.nba.core.exception.invalidData.UserInvalidDataException;
import com.nba.user.User;
import com.nba.user.UserShortDto;
import com.nba.user.UserService;
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
        if (userService.existsByUsername(registerRequest.username()))
            throw new UserInvalidDataException("User with teamName " + registerRequest.username() + " already exist");
        User newUser = authMapper.toUserEntity(registerRequest);
        return userService.saveToDataBase(newUser);
    }
}
