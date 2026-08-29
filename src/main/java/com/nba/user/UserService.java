package com.nba.user;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User getUserByUsername(String username);

    UserShortDto saveToDataBaseAndReturnDto(User user);



    UserShortDto getUserProfileById(Long userId);

    UserShortDto partialUpdateUserProfileById(Long userId, UserUpdateRequest updateRequest);

    void deleteUserById(Long userId);

    List<UserShortDto> getAllUsers();

    void passwordUpdateByUserIdForCurrentUser(Long userId, PasswordUpdateRequest passwordUpdateRequest);

    List<UserFullDto> searchUsers(UserSearchFilter filter);

    UserShortDto createUserAccount(UserCreationRequest request);

    Optional<User> findByEmail(String email);

    User saveToDatabase(User user);

    User createOAuth2User(String username, String email);

    User findByUsernameOrEmail(String login);

    void validateUsernameIsFree(String username);
    void validateEmailIsFree(String email);
    void setUserPasswordById(Long userId, String newPassword);
}
