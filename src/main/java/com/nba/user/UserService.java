package com.nba.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    User getUserByUsername(String username);

    UserShortDto saveToDataBaseAndReturnDto(User user);

    UserShortDto getUserProfileById(Long userId);

    UserShortDto partialUpdateUserProfileById(Long userId, UserUpdateRequest updateRequest);

    void deleteUserById(Long userId);

    Page<UserShortDto> getAllUsers(Pageable pageable);

    void passwordUpdateByUserIdForCurrentUser(Long userId, PasswordUpdateRequest passwordUpdateRequest);

    Page<UserFullDto> searchUsers(UserSearchFilter filter,Pageable pageable);

    UserShortDto createUserAccount(UserCreationRequest request);

    Optional<User> findByEmail(String email);

    User saveToDatabase(User user);

    User createOAuth2User(String username, String email);

    User findByUsernameOrEmail(String login);

    void validateUsernameIsFree(String username);
    void validateEmailIsFree(String email);
    void setUserPasswordById(Long userId, String newPassword);


}
