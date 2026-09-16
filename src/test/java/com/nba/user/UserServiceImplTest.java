package com.nba.user;

import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.core.exception.notFound.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;


    private static final Long NON_EXISTING_USER_ID = 9999L;
    private static final Long EXISTING_USER_ID = 1L;
    private static final String NEW_USERNAME = "nikitaUpdated";
    private static final String NEW_EMAIL = "nikitanewEmail@gmail.com";
    private static final String OLD_PASSWORD = "oldPassword";
    private static final String OLD_ENCODED_PASSWORD = "$2oldEncodedPassword";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String WRONG_PASSWORD = "wrongPassword";

    @Test
    @DisplayName("should return profile by id")
    void shouldGetProfileById() {
        User user = createUser();

        UserShortDto expectedUserShortDto = createUserShortDto();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        when(userMapper.toUserShortDto(user))
                .thenReturn(expectedUserShortDto);

        UserShortDto result = userService.getUserProfileById(EXISTING_USER_ID);

        assertEquals(expectedUserShortDto, result);

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should return UserNotFoundException when get profile with non-existing user id")
    void shouldThrowUserNotFoundExceptionWhenGetProfileByNonExistingUserId() {
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserProfileById(NON_EXISTING_USER_ID)
        );

        assertEquals("User with id 9999 not found", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(NON_EXISTING_USER_ID);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should not throw exception when username is free")
    void shouldNotThrowExceptionWhenUsernameIsFree() {
        String username = "nikita";

        when(userRepository.existsByUsername(username))
                .thenReturn(false);

        assertDoesNotThrow(() -> userService.validateUsernameIsFree(username));
        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException exception when username already exists")
    void shouldThrowInvalidUserDataExceptionWhenUsernameAlreadyExists() {
        String username = "nikita";

        when(userRepository.existsByUsername(username))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.validateUsernameIsFree(username));

        assertEquals("User with username nikita already exists",
                exception.getMessage());

        verify(userRepository).existsByUsername(username);
    }

    @Test
    @DisplayName("should not throw exception when email is free")
    void shouldNotThrowExceptionWhenEmailIsFree() {
        String email = "nikita@gmail.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(false);

        assertDoesNotThrow(() -> userService.validateEmailIsFree(email));

        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException exception when email already exists")
    void shouldThrowInvalidUserDataExceptionExceptionWhenEmailAlreadyExists() {
        String email = "nikita@gmail.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.validateEmailIsFree(email));

        assertEquals("User with email nikita@gmail.com already exists",
                exception.getMessage());

        verify(userRepository).existsByEmail(email);
    }

    @Test
    @DisplayName("should set new encoded password by user id")
    void shouldSetNewEncodedPasswordByUserId() {
        User user = createUser();
        user.setPassword(OLD_PASSWORD);


        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        userService.setUserPasswordById(EXISTING_USER_ID, NEW_PASSWORD);

        assertEquals(ENCODED_PASSWORD, user.getPassword());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("should throw exception when setting password for non existing user")
    void shouldThrowUserNotFoundExceptionWhenSettingPasswordForNonExistingUser() {

        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.setUserPasswordById(NON_EXISTING_USER_ID, "newPassword"));

        assertEquals("User with id 9999 not found",
                exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(NON_EXISTING_USER_ID);
        verifyNoInteractions(passwordEncoder);

    }

    @Test
    @DisplayName("should get user by username")
    void shouldGetUserByUsername() {
        User user = createUser();
        String username = "nikita";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
        User result = userService.getUserByUsername(username);

        assertEquals(user.getId(),
                result.getId());

        assertEquals(user.getUsername(),
                result.getUsername());

        assertEquals(user.getEmail(),
                result.getEmail());

        verify(userRepository).findByUsername(username);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when get user by non existing username")
    void shouldThrowInvalidUserDataExceptionWhenGetUserByNonExistingUsername() {
        String nonExistingUsername = "nonExistingUsername";

        when(userRepository.findByUsername(nonExistingUsername))
                .thenReturn(Optional.empty());

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.getUserByUsername(nonExistingUsername));

        assertEquals("User with username nonExistingUsername not found",
                exception.getMessage());

        verify(userRepository).findByUsername(nonExistingUsername);
    }


    @Test
    @DisplayName("should save user to database and return user short DTO")
    void shouldSaveUserToDatabaseAndReturnUserShortDTO() {
        User user = createUser();
        UserShortDto expectedUserShortDto = createUserShortDto();

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toUserShortDto(user))
                .thenReturn(expectedUserShortDto);

        UserShortDto result = userService.saveToDataBaseAndReturnDto(user);

        assertEquals(expectedUserShortDto, result);

        verify(userRepository).save(user);

        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should update username and email")
    void shouldUpdateUsernameAndEmail() {
        User user = createUser();

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        UserShortDto expectedUserShortDto = UserShortDto.builder()
                .id(user.getId())
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME))
                .thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL))
                .thenReturn(false);
        when(userMapper.toUserShortDto(user))
                .thenReturn(expectedUserShortDto);

        UserShortDto result = userService.partialUpdateUserProfileById(EXISTING_USER_ID,
                updateRequest);

        assertEquals(expectedUserShortDto, result);
        assertEquals(NEW_USERNAME, user.getUsername());
        assertEquals(NEW_EMAIL, user.getEmail());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository).existsByEmail(NEW_EMAIL);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should update only username")
    void shouldUpdateOnlyUsername() {
        User user = createUser();

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(null)
                .build();

        UserShortDto expectedUserShortDTO = UserShortDto.builder()
                .id(user.getId())
                .username(NEW_USERNAME)
                .email(user.getEmail())
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME))
                .thenReturn(false);
        when(userMapper.toUserShortDto(user))
                .thenReturn(expectedUserShortDTO);

        UserShortDto result = userService.partialUpdateUserProfileById(EXISTING_USER_ID, userUpdateRequest);

        assertEquals(expectedUserShortDTO.id(),
                result.id());

        assertEquals(expectedUserShortDTO.username(),
                result.username());

        assertEquals(expectedUserShortDTO.email(),
                result.email());

        assertEquals(NEW_USERNAME, user.getUsername());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verifyNoMoreInteractions(userRepository);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should update only email")
    void shouldUpdateOnlyEmail() {
        User user = createUser();

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .username(null)
                .email(NEW_EMAIL)
                .build();

        UserShortDto expectedUserShortDTO = UserShortDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(userRepository.existsByEmail(NEW_EMAIL))
                .thenReturn(false);
        when(userMapper.toUserShortDto(user))
                .thenReturn(expectedUserShortDTO);

        UserShortDto result = userService.partialUpdateUserProfileById(EXISTING_USER_ID, userUpdateRequest);

        assertEquals(expectedUserShortDTO.id(),
                result.id());

        assertEquals(expectedUserShortDTO.username(),
                result.username());

        assertEquals(expectedUserShortDTO.email(),
                result.email());

        assertEquals(NEW_EMAIL, user.getEmail());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).existsByEmail(NEW_EMAIL);
        verifyNoMoreInteractions(userRepository);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when new username already exists")
    void shouldThrowInvalidUserDataExceptionWhenNewUsernameAlreadyExists() {
        User user = createUser();
        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.partialUpdateUserProfileById(EXISTING_USER_ID, userUpdateRequest));
        assertEquals("User with username nikitaUpdated already exists", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);

    }

    @Test
    @DisplayName("should throw InvalidUserDataException when new email already exists")
    void shouldThrowInvalidUserDataExceptionWhenNewEmailAlreadyExists() {
        User user = createUser();

        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        when(userRepository.existsByUsername(NEW_USERNAME))
                .thenReturn(false);

        when(userRepository.existsByEmail(NEW_EMAIL))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.partialUpdateUserProfileById(EXISTING_USER_ID, userUpdateRequest));

        assertEquals("User with email nikitanewEmail@gmail.com already exists", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).existsByUsername(NEW_USERNAME);
        verify(userRepository).existsByEmail(NEW_EMAIL);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);

    }

    @Test
    @DisplayName("should throw UserNotFoundException when updating user by non existing user id")
    void shouldThrowUserNotFoundExceptionWhenUpdatingByNonExistingUserId() {
        UserUpdateRequest userUpdateRequest = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .build();

        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.partialUpdateUserProfileById(NON_EXISTING_USER_ID, userUpdateRequest));

        assertEquals("User with id 9999 not found", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(NON_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should delete by user id")
    void shouldDeleteById() {
        User user = createUser();
        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        userService.deleteUserById(EXISTING_USER_ID);

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when deleting user by non-existing id")
    void shouldThrowUserNotFoundExceptionWhenDeletingUserByNonExistingId() {

        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.deleteUserById(NON_EXISTING_USER_ID));

        assertEquals("User with id 9999 not found", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(NON_EXISTING_USER_ID);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("should return all users")
    void shouldReturnAllUsers() {
        User user1 = createUser();

        User user2 = User.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .build();

        UserShortDto userShortDto1 = createUserShortDto();

        UserShortDto userShortDto2 = UserShortDto.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .build();

        Page<User> userPage = new PageImpl<>(List.of(user1, user2));

        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable))
                .thenReturn(userPage);

        when(userMapper.toUserShortDto(user1))
                .thenReturn(userShortDto1);

        when(userMapper.toUserShortDto(user2))
                .thenReturn(userShortDto2);

        Page<UserShortDto> result = userService.getAllUsers(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(userShortDto1, userShortDto2), result.getContent());

        verify(userRepository).findAll(pageable);
        verify(userMapper).toUserShortDto(user1);
        verify(userMapper).toUserShortDto(user2);

    }

    @Test
    @DisplayName("should return empty page")
    void shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> emptyUserPage = Page.empty(pageable);

        when(userRepository.findAll(pageable))
                .thenReturn(emptyUserPage);

        Page<UserShortDto> result = userService.getAllUsers(pageable);

        assertTrue(result.isEmpty());

        verify(userRepository).findAll(pageable);
        verifyNoInteractions(userMapper);

    }


    @Test
    @DisplayName("should update new password when user has not real password")
    void shouldUpdateNewPasswordWhenUserHasNotRealPassword() {
        User user = createUser();
        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .newPassword(NEW_PASSWORD)
                .build();
        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, passwordUpdateRequest);

        assertEquals(ENCODED_PASSWORD,
                user.getPassword());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("should update new password when user real password")
    void shouldUpdatePasswordWhenUserHasRealPassword() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD))
                .thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, OLD_ENCODED_PASSWORD))
                .thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD))
                .thenReturn(ENCODED_PASSWORD);

        userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, passwordUpdateRequest);

        assertEquals(ENCODED_PASSWORD, user.getPassword());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(passwordEncoder).matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD);
        verify(passwordEncoder).matches(NEW_PASSWORD, OLD_ENCODED_PASSWORD);
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when current password is null")
    void shouldThrowInvalidUserDataExceptionWhenCurrentPasswordNull() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .currentPassword(null)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, passwordUpdateRequest));

        assertEquals("Current password is required", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when current password is incorrect")
    void shouldThrowInvalidUserDataExceptionWhenCurrentPasswordIsIncorrect() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .currentPassword(WRONG_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        when(passwordEncoder.matches(WRONG_PASSWORD, OLD_ENCODED_PASSWORD))
                .thenReturn(false);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, passwordUpdateRequest));

        assertEquals("Current password is incorrect", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(passwordEncoder).matches(WRONG_PASSWORD, OLD_ENCODED_PASSWORD);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when new password is equal to old password")
    void shouldThrowInvalidUserDataExceptionWhenNewPasswordIsEqualToOldPassword() {
        User user = createUser();

        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(OLD_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID))
                .thenReturn(user);

        when(passwordEncoder.matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, passwordUpdateRequest));

        assertEquals("The new password must be different from the current password", exception.getMessage());

        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);

        verify(passwordEncoder, times(2)).matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD);

        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when updating password by non existing id")
    void shouldThrowUserNotFoundExceptionWhenUpdatingPasswordByNonExistingId() {
        PasswordUpdateRequest passwordUpdateRequest = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(NON_EXISTING_USER_ID, passwordUpdateRequest));

        assertEquals("User with id 9999 not found", exception.getMessage());
        verify(userRepository).findUserByIdOrThrow404(NON_EXISTING_USER_ID);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should return all users when search filter is empty")
    void shouldReturnAllUsersWhenFilterIsEmpty() {
        User user1 = createUser();
        User user2 = User.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .build();

        UserFullDto userFullDto1 = UserFullDto.builder()
                .id(1L)
                .username("nikita")
                .email("nikita@gmail.com")
                .build();

        UserFullDto userFullDto2 = UserFullDto.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .build();

        UserSearchFilter userSearchFilter = new UserSearchFilter(null, null, null, null, null, null, null);

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> userPage = new PageImpl<>(List.of(user1, user2));

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);

        when(userMapper.toUserFullDto(user1))
                .thenReturn(userFullDto1);

        when(userMapper.toUserFullDto(user2))
                .thenReturn(userFullDto2);

        Page<UserFullDto> result = userService.searchUsers(userSearchFilter, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of(userFullDto1, userFullDto2), result.getContent());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toUserFullDto(user1);
        verify(userMapper).toUserFullDto(user2);

    }

    @Test
    @DisplayName("should return users when full search filter is provided")
    void shouldReturnUsersWhenFullSearchFilterIsProvided() {
        User user = User.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .roles(Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.of(2026, 9, 10, 10, 10))
                .lastLogin(LocalDateTime.of(2026, 9, 10, 10, 10))
                .build();

        UserFullDto userFullDto = UserFullDto.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .roles(List.of(
                        UserRole.ROLE_ADMIN.toString(),
                        UserRole.ROLE_USER.toString()
                ))
                .registeredAt("2026-09-10T00:00")
                .lastLogin("2026-09-10T00:00")
                .build();

        UserSearchFilter filter = new UserSearchFilter(
                "s",
                "gmail",
                Set.of(UserRole.ROLE_ADMIN),
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 10)
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);

        when(userMapper.toUserFullDto(user))
                .thenReturn(userFullDto);

        Page<UserFullDto> result = userService.searchUsers(filter, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of(userFullDto), result.getContent());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toUserFullDto(user);
    }

    @Test
    @DisplayName("should return users when partial search filter is provided")
    void shouldReturnUsersWhenPartialSearchFilterIsProvided() {
        User user = User.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        UserFullDto userFullDto = UserFullDto.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .roles(List.of(UserRole.ROLE_ADMIN.toString()))
                .build();

        UserSearchFilter filter = new UserSearchFilter(
                "s",
                null,
                Set.of(UserRole.ROLE_ADMIN),
                null,
                null,
                null,
                null
        );

        Pageable pageable = PageRequest.of(0, 10);

        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(userPage);

        when(userMapper.toUserFullDto(user))
                .thenReturn(userFullDto);

        Page<UserFullDto> result = userService.searchUsers(filter, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of(userFullDto), result.getContent());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toUserFullDto(user);
    }

    @Test
    @DisplayName("should create user account")
    void shouldCreateUserAccount() {
        UserCreationRequest userCreationRequest = createUserCreationRequest();
        User user = createUser();
        UserShortDto userShortDto = createUserShortDto();

        when(userRepository.existsByUsername(userCreationRequest.username()))
                .thenReturn(false);

        when(userRepository.existsByEmail(userCreationRequest.email()))
                .thenReturn(false);

        when(userMapper.toUserEntity(userCreationRequest))
                .thenReturn(user);

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toUserShortDto(user))
                .thenReturn(userShortDto);

        UserShortDto result = userService.createUserAccount(userCreationRequest);

        assertEquals(userShortDto, result);

        verify(userRepository).existsByUsername(userCreationRequest.username());
        verify(userRepository).existsByEmail(userCreationRequest.email());
        verify(userMapper).toUserEntity(userCreationRequest);
        verify(userRepository).save(user);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when create account and username already exists")
    void shouldThrowInvalidUserDataExceptionWhenCreateAccountAndUsernameAlreadyExists() {
        UserCreationRequest userCreationRequest = createUserCreationRequest();

        when(userRepository.existsByUsername(userCreationRequest.username()))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.createUserAccount(userCreationRequest));

        assertEquals("User with username nikita already exists", exception.getMessage());

        verify(userRepository).existsByUsername(userCreationRequest.username());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when create account and email already exists")
    void shouldThrowInvalidUserDataExceptionWhenCreateAccountAndEmailAlreadyExists() {
        UserCreationRequest userCreationRequest = createUserCreationRequest();

        when(userRepository.existsByUsername(userCreationRequest.username()))
                .thenReturn(false);
        when(userRepository.existsByEmail(userCreationRequest.email()))
                .thenReturn(true);

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.createUserAccount(userCreationRequest));

        assertEquals("User with email nikita@gmail.com already exists", exception.getMessage());

        verify(userRepository).existsByUsername(userCreationRequest.username());
        verify(userRepository).existsByEmail(userCreationRequest.email());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should save user to database")
    void shouldSaveUserToDatabase() {
        User user = createUser();
        when(userRepository.save(user))
                .thenReturn(user);
        User result = userService.saveToDatabase(user);

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("should create Oauth2 user when username does not exist")
    void shouldCreateOauth2UserWhenUsernameDoesNotExist() {
        User user = createUser();

        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(false);

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createOAuth2User(user.getUsername(), user.getEmail());

        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(Set.of(UserRole.ROLE_USER), result.getRoles());
        assertNotNull(result.getRegisteredAt());

        verify(userRepository).existsByUsername(user.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should create Oauth2 user and add characters to username when username already exists")
    void shouldCreateOauth2UserWhenUsernameAlreadyExist() {
        User user = createUser();
        when(userRepository.existsByUsername(user.getUsername()))
                .thenReturn(true);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createOAuth2User(user.getUsername(), user.getEmail());

        assertNotEquals(user.getUsername(), result.getUsername());
        assertTrue(result.getUsername().startsWith(user.getUsername() + "_"));
        assertEquals((user.getUsername().length() + 6), result.getUsername().length());
        assertEquals(Set.of(UserRole.ROLE_USER), result.getRoles());
        assertNotNull(result.getRegisteredAt());

        verify(userRepository).existsByUsername(user.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should find and return user by username")
    void shouldFindAndReturnUserByUsername() {
        User user = createUser();
        when(userRepository.findByUsernameOrEmail(user.getUsername()))
                .thenReturn(Optional.of(user));

        User result = userService.findByUsernameOrEmail(user.getUsername());

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findByUsernameOrEmail(user.getUsername());
    }

    @Test
    @DisplayName("should find and return user by email")
    void shouldFindAndReturnUserByEmail() {
        User user = createUser();
        when((userRepository.findByUsernameOrEmail(user.getEmail())))
                .thenReturn(Optional.of(user));

        User result = userService.findByUsernameOrEmail(user.getEmail());

        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());

        verify(userRepository).findByUsernameOrEmail(user.getEmail());
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when user not found by username and email")
    void shouldThrowInvalidUserDataExceptionWhenUserNotFoundByUsernameAndEmail() {
        String login = "unknown";
        when(userRepository.findByUsernameOrEmail(login))
                .thenReturn(Optional.empty());

        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class,
                () -> userService.findByUsernameOrEmail(login));

        assertEquals("User not found with login/email: unknown", exception.getMessage());

        verify(userRepository).findByUsernameOrEmail(login);
    }


    private User createUser() {
        return User.builder()
                .id(1L)
                .username("nikita")
                .email("nikita@gmail.com")
                .password(NEW_PASSWORD)
                .build();
    }

    private UserShortDto createUserShortDto() {
        return UserShortDto.builder()
                .id(1L)
                .username("nikita")
                .email("nikita@gmail.com")
                .build();
    }

    private UserCreationRequest createUserCreationRequest() {
        return UserCreationRequest.builder()
                .username("nikita")
                .email("nikita@gmail.com")
                .password(NEW_PASSWORD)
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
    }


}

