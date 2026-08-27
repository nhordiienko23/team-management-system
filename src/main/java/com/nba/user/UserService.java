package com.nba.user;

import java.util.List;

public interface UserService {
    User getUserByUsername(String username);

    UserDto saveToDataBase(User user);

    boolean existsByUsername(String username);

    UserDto getMyProfile(Long userId);

    UserDto partialUpdateUserProfileById(Long userId, UpdateRequest updateRequest);

    void deleteUserById(Long userId);

    List<UserDto> getAllUsers();

    void passwordUpdate(Long userId, PasswordUpdateRequest passwordUpdateRequest);
    List<ResponseSearchUser> searchUsers(UserSearchFilter filter);

}
