package com.nba.user;

import java.util.List;

public interface UserService {
    User getUserByUsername(String username);

    UserShortDto saveToDataBase(User user);

    boolean existsByUsername(String username);

    UserShortDto getUserById(Long userId);

    UserShortDto partialUpdateUserProfileById(Long userId, UserUpdateRequest updateRequest);

    void deleteUserById(Long userId);

    List<UserShortDto> getAllUsers();

    void passwordUpdateByUserId(Long userId, PasswordUpdateRequest passwordUpdateRequest);
    List<UserFullDto> searchUsers(UserSearchFilter filter);

    UserShortDto createUserAccount(UserCreationRequest request);
}
