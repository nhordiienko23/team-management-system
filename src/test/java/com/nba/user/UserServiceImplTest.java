package com.nba.user;

import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.core.exception.notFound.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long NON_EXISTING_USER_ID = 9999L;
    private static final Long EXISTING_USER_ID = 1L;
    private static final String NEW_USERNAME = "nikitaUpdated";
    private static final String NEW_EMAIL = "nikitanewEmail@gmail.com";
    private static final String OLD_PASSWORD = "oldPassword";
    private static final String OLD_ENCODED_PASSWORD = "$2oldEncodedPassword";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String WRONG_PASSWORD = "wrongPassword";

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";
    private static final String CUSTOM_ISSUER = "http://localhost:9000";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "customAuthIssuer", CUSTOM_ISSUER);
    }

    @Test
    @DisplayName("should return profile by id")
    void shouldGetProfileById() {
        User user = createUser();
        UserShortDto expected = createUserShortDto();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserShortDto(user)).thenReturn(expected);

        UserShortDto result = userService.getUserProfileById(EXISTING_USER_ID);

        assertEquals(expected, result);
        verify(userRepository).findUserByIdOrThrow404(EXISTING_USER_ID);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when profile does not exist")
    void shouldThrowWhenProfileDoesNotExist() {
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserProfileById(NON_EXISTING_USER_ID)
        );

        assertEquals("User with id 9999 not found", exception.getMessage());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should return administrator details with sorted distinct provider names")
    void shouldReturnAdminDetailsWithFriendlyLinkedProviderNames() {
        User user = createUser();
        UserFullDto fullDto = createFullDto(user, null);

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserFullDto(user)).thenReturn(fullDto);
        when(externalIdentityRepository.findAllByUser_Id(EXISTING_USER_ID))
                .thenReturn(List.of(
                        identity(GOOGLE_ISSUER, "google-sub-1", user),
                        identity("google", "google-sub-2", user),
                        identity("github", "github-sub", user),
                        identity(CUSTOM_ISSUER, "custom-sub", user),
                        identity("https://unknown.example/issuer", "other-sub", user)
                ));

        AdminUserDetailDto result = userService.getAdminUserDetailsById(EXISTING_USER_ID);

        assertEquals(EXISTING_USER_ID, result.id());
        assertEquals("nikita", result.username());
        assertEquals("nikita@gmail.com", result.email());
        assertEquals(List.of(UserRole.ROLE_USER.name()), result.roles());
        assertEquals("2026-09-10T10:10", result.registeredAt());
        assertNull(result.lastLoginAt());
        assertEquals(List.of(
                "Custom authorization server",
                "GitHub",
                "Google",
                "Other sign-in provider"
        ), result.linkedProviders());

        verify(externalIdentityRepository).findAllByUser_Id(EXISTING_USER_ID);
    }

    @Test
    @DisplayName("should return administrator details with last login timestamp")
    void shouldReturnAdminDetailsWithLastLogin() {
        User user = createUser();
        UserFullDto fullDto = UserFullDto.builder()
                .id(EXISTING_USER_ID)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(List.of(UserRole.ROLE_USER.name()))
                .registeredAt("2026-09-10T10:10")
                .lastLogin("2026-09-11T12:30")
                .linkedProviders(List.of())
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserFullDto(user)).thenReturn(fullDto);
        when(externalIdentityRepository.findAllByUser_Id(EXISTING_USER_ID)).thenReturn(List.of());

        AdminUserDetailDto result = userService.getAdminUserDetailsById(EXISTING_USER_ID);

        assertEquals("2026-09-11T12:30", result.lastLoginAt());
        assertEquals(List.of(), result.linkedProviders());
    }

    @Test
    @DisplayName("should map configured custom OIDC issuer to a safe provider name")
    void shouldMapConfiguredCustomOidcIssuerToProviderName() {
        String configuredIssuer = "https://login.example.com";
        ReflectionTestUtils.setField(userService, "customAuthIssuer", configuredIssuer);

        User user = createUser();
        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserFullDto(user)).thenReturn(createFullDto(user, null));
        when(externalIdentityRepository.findAllByUser_Id(EXISTING_USER_ID))
                .thenReturn(List.of(identity(configuredIssuer, "private-sub", user)));

        AdminUserDetailDto result = userService.getAdminUserDetailsById(EXISTING_USER_ID);

        assertEquals(List.of("Custom authorization server"), result.linkedProviders());
    }

    @Test
    @DisplayName("should return an empty provider list when no identities are linked")
    void shouldReturnEmptyProviderListWhenNoIdentitiesAreLinked() {
        User user = createUser();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserFullDto(user)).thenReturn(createFullDto(user, null));
        when(externalIdentityRepository.findAllByUser_Id(EXISTING_USER_ID)).thenReturn(List.of());

        AdminUserDetailDto result = userService.getAdminUserDetailsById(EXISTING_USER_ID);

        assertNull(result.lastLoginAt());
        assertEquals(List.of(), result.linkedProviders());
    }

    @Test
    @DisplayName("should throw UserNotFoundException when administrator details do not exist")
    void shouldThrowWhenAdminDetailsUserDoesNotExist() {
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getAdminUserDetailsById(NON_EXISTING_USER_ID)
        );

        verifyNoInteractions(userMapper, externalIdentityRepository);
    }

    @Test
    @DisplayName("should not throw exception when username is free")
    void shouldNotThrowWhenUsernameIsFree() {
        when(userRepository.existsByUsername("nikita")).thenReturn(false);

        assertDoesNotThrow(() -> userService.validateUsernameIsFree("nikita"));

        verify(userRepository).existsByUsername("nikita");
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when username already exists")
    void shouldThrowWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("nikita")).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateUsernameIsFree("nikita")
        );

        assertEquals("User with username nikita already exists", exception.getMessage());
        verify(userRepository).existsByUsername("nikita");
    }

    @Test
    @DisplayName("should not throw exception when email is free")
    void shouldNotThrowWhenEmailIsFree() {
        when(userRepository.existsByEmail("nikita@gmail.com")).thenReturn(false);

        assertDoesNotThrow(() -> userService.validateEmailIsFree("nikita@gmail.com"));

        verify(userRepository).existsByEmail("nikita@gmail.com");
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when email already exists")
    void shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("nikita@gmail.com")).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.validateEmailIsFree("nikita@gmail.com")
        );

        assertEquals("User with email nikita@gmail.com already exists", exception.getMessage());
        verify(userRepository).existsByEmail("nikita@gmail.com");
    }

    @Test
    @DisplayName("should set new encoded password by user id")
    void shouldSetNewEncodedPasswordByUserId() {
        User user = createUser();
        user.setPassword(OLD_PASSWORD);

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.setUserPasswordById(EXISTING_USER_ID, NEW_PASSWORD);

        assertEquals(ENCODED_PASSWORD, user.getPassword());
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when setting password for missing user")
    void shouldThrowWhenSettingPasswordForMissingUser() {
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        assertThrows(
                UserNotFoundException.class,
                () -> userService.setUserPasswordById(NON_EXISTING_USER_ID, NEW_PASSWORD)
        );

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should get user by username")
    void shouldGetUserByUsername() {
        User user = createUser();
        when(userRepository.findByUsername("nikita")).thenReturn(Optional.of(user));

        User result = userService.getUserByUsername("nikita");

        assertSame(user, result);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when username is not found")
    void shouldThrowWhenUsernameIsNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.getUserByUsername("unknown")
        );

        assertEquals("User with username unknown not found", exception.getMessage());
    }

    @Test
    @DisplayName("should save user and return short DTO")
    void shouldSaveUserAndReturnShortDto() {
        User user = createUser();
        UserShortDto expected = createUserShortDto();

        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserShortDto(user)).thenReturn(expected);

        assertEquals(expected, userService.saveToDataBaseAndReturnDto(user));

        verify(userRepository).save(user);
        verify(userMapper).toUserShortDto(user);
    }

    @Test
    @DisplayName("should update username and email")
    void shouldUpdateUsernameAndEmail() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();
        UserShortDto expected = UserShortDto.builder()
                .id(EXISTING_USER_ID)
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(userMapper.toUserShortDto(user)).thenReturn(expected);

        UserShortDto result = userService.partialUpdateUserProfileById(EXISTING_USER_ID, request);

        assertEquals(expected, result);
        assertEquals(NEW_USERNAME, user.getUsername());
        assertEquals(NEW_EMAIL, user.getEmail());
    }

    @Test
    @DisplayName("should update only username")
    void shouldUpdateOnlyUsername() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userMapper.toUserShortDto(user)).thenAnswer(invocation -> createUserShortDtoFor(user));

        userService.partialUpdateUserProfileById(EXISTING_USER_ID, request);

        assertEquals(NEW_USERNAME, user.getUsername());
        assertEquals("nikita@gmail.com", user.getEmail());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("should update only email")
    void shouldUpdateOnlyEmail() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(false);
        when(userMapper.toUserShortDto(user)).thenAnswer(invocation -> createUserShortDtoFor(user));

        userService.partialUpdateUserProfileById(EXISTING_USER_ID, request);

        assertEquals("nikita", user.getUsername());
        assertEquals(NEW_EMAIL, user.getEmail());
        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    @DisplayName("should not check uniqueness when profile fields are unchanged")
    void shouldNotCheckUniquenessWhenFieldsAreUnchanged() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userMapper.toUserShortDto(user)).thenReturn(createUserShortDto());

        userService.partialUpdateUserProfileById(EXISTING_USER_ID, request);

        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when new username already exists")
    void shouldThrowWhenNewUsernameAlreadyExists() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.partialUpdateUserProfileById(EXISTING_USER_ID, request)
        );

        assertEquals("User with username nikitaUpdated already exists", exception.getMessage());
        verify(userRepository, never()).existsByEmail(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when new email already exists")
    void shouldThrowWhenNewEmailAlreadyExists() {
        User user = createUser();
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(NEW_USERNAME)
                .email(NEW_EMAIL)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(userRepository.existsByUsername(NEW_USERNAME)).thenReturn(false);
        when(userRepository.existsByEmail(NEW_EMAIL)).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.partialUpdateUserProfileById(EXISTING_USER_ID, request)
        );

        assertEquals("User with email nikitanewEmail@gmail.com already exists", exception.getMessage());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when updating missing user")
    void shouldThrowWhenUpdatingMissingUser() {
        UserUpdateRequest request = UserUpdateRequest.builder().username(NEW_USERNAME).build();

        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        assertThrows(
                UserNotFoundException.class,
                () -> userService.partialUpdateUserProfileById(NON_EXISTING_USER_ID, request)
        );

        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should delete user by id")
    void shouldDeleteUserById() {
        User user = createUser();
        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);

        userService.deleteUserById(EXISTING_USER_ID);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("should throw UserNotFoundException when deleting missing user")
    void shouldThrowWhenDeletingMissingUser() {
        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUserById(NON_EXISTING_USER_ID)
        );

        verify(userRepository, never()).delete((User) any());
    }

    @Test
    @DisplayName("should return all users")
    void shouldReturnAllUsers() {
        User user1 = createUser();
        User user2 = User.builder().id(2L).username("sveta").email("sveta@gmail.com").build();

        UserShortDto dto1 = createUserShortDto();
        UserShortDto dto2 = UserShortDto.builder()
                .id(2L).username("sveta").email("sveta@gmail.com").build();

        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user1, user2)));
        when(userMapper.toUserShortDto(user1)).thenReturn(dto1);
        when(userMapper.toUserShortDto(user2)).thenReturn(dto2);

        Page<UserShortDto> result = userService.getAllUsers(pageable);

        assertEquals(List.of(dto1, dto2), result.getContent());
        assertEquals(2, result.getTotalElements());
    }

    @Test
    @DisplayName("should return empty page without mapping users")
    void shouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(Page.empty(pageable));

        Page<UserShortDto> result = userService.getAllUsers(pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should update password when user has no existing BCrypt password")
    void shouldUpdatePasswordWhenUserHasNoRealPassword() {
        User user = createUser();
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, request);

        assertEquals(ENCODED_PASSWORD, user.getPassword());
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("should update password when current BCrypt password is correct")
    void shouldUpdatePasswordWhenCurrentPasswordIsCorrect() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, OLD_ENCODED_PASSWORD)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, request);

        assertEquals(ENCODED_PASSWORD, user.getPassword());
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("should reject password update when current password is missing")
    void shouldRejectWhenCurrentPasswordIsMissing() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, request)
        );

        assertEquals("Current password is required", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should reject password update when current password is incorrect")
    void shouldRejectWhenCurrentPasswordIsIncorrect() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword(WRONG_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(passwordEncoder.matches(WRONG_PASSWORD, OLD_ENCODED_PASSWORD)).thenReturn(false);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, request)
        );

        assertEquals("Current password is incorrect", exception.getMessage());
        verify(passwordEncoder).matches(WRONG_PASSWORD, OLD_ENCODED_PASSWORD);
        verifyNoMoreInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should reject password update when new password equals current password")
    void shouldRejectWhenNewPasswordEqualsCurrentPassword() {
        User user = createUser();
        user.setPassword(OLD_ENCODED_PASSWORD);

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(OLD_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(EXISTING_USER_ID)).thenReturn(user);
        when(passwordEncoder.matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD)).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(EXISTING_USER_ID, request)
        );

        assertEquals(
                "The new password must be different from the current password",
                exception.getMessage()
        );
        verify(passwordEncoder, times(2)).matches(OLD_PASSWORD, OLD_ENCODED_PASSWORD);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("should throw UserNotFoundException when updating password for missing user")
    void shouldThrowWhenUpdatingPasswordForMissingUser() {
        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword(OLD_PASSWORD)
                .newPassword(NEW_PASSWORD)
                .build();

        when(userRepository.findUserByIdOrThrow404(NON_EXISTING_USER_ID))
                .thenThrow(new UserNotFoundException(NON_EXISTING_USER_ID));

        assertThrows(
                UserNotFoundException.class,
                () -> userService.passwordUpdateByUserIdForCurrentUser(NON_EXISTING_USER_ID, request)
        );

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("should return users with provider names from one batch query")
    void shouldReturnUsersWithProviderNamesFromBatchQuery() {
        User user1 = createUser();
        User user2 = User.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .build();

        UserFullDto dto1 = createFullDto(user1, null);
        UserFullDto dto2 = createFullDto(user2, null);

        Pageable pageable = PageRequest.of(0, 10);
        UserSearchFilter filter = emptySearchFilter();

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user1, user2)));
        when(userMapper.toUserFullDto(user1)).thenReturn(dto1);
        when(userMapper.toUserFullDto(user2)).thenReturn(dto2);
        when(externalIdentityRepository.findAllByUser_IdIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        identity(GOOGLE_ISSUER, "google-sub", user1),
                        identity("github", "github-sub", user1),
                        identity("github", "github-sub-2", user1),
                        identity(CUSTOM_ISSUER, "custom-sub", user2),
                        identity("unknown", "other-sub", user2)
                ));

        Page<UserFullDto> result = userService.searchUsers(filter, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(List.of("GitHub", "Google"), result.getContent().get(0).linkedProviders());
        assertEquals(
                List.of("Custom authorization server", "Other sign-in provider"),
                result.getContent().get(1).linkedProviders()
        );

        verify(externalIdentityRepository).findAllByUser_IdIn(List.of(1L, 2L));
        verify(externalIdentityRepository, never()).findAllByUser_Id(any());
    }

    @Test
    @DisplayName("should return users with empty provider lists when no identities are linked")
    void shouldReturnUsersWithEmptyProviderLists() {
        User user = createUser();
        UserFullDto dto = createFullDto(user, null);
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toUserFullDto(user)).thenReturn(dto);
        when(externalIdentityRepository.findAllByUser_IdIn(List.of(EXISTING_USER_ID)))
                .thenReturn(List.of());

        Page<UserFullDto> result = userService.searchUsers(emptySearchFilter(), pageable);

        assertEquals(List.of(), result.getContent().get(0).linkedProviders());
    }

    @Test
    @DisplayName("should not query external identities when search result is empty")
    void shouldNotQueryExternalIdentitiesWhenSearchResultIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<UserFullDto> result = userService.searchUsers(emptySearchFilter(), pageable);

        assertTrue(result.isEmpty());
        verifyNoInteractions(userMapper, externalIdentityRepository);
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

        UserFullDto dto = UserFullDto.builder()
                .id(2L)
                .username("sveta")
                .email("sveta@gmail.com")
                .roles(List.of(UserRole.ROLE_ADMIN.toString(), UserRole.ROLE_USER.toString()))
                .registeredAt("2026-09-10T00:00")
                .lastLogin("2026-09-10T00:00")
                .linkedProviders(List.of())
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
        when(userRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toUserFullDto(user)).thenReturn(dto);
        when(externalIdentityRepository.findAllByUser_IdIn(List.of(2L))).thenReturn(List.of());

        Page<UserFullDto> result = userService.searchUsers(filter, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(List.of(dto), result.getContent());
    }

    @Test
    @DisplayName("should create user account")
    void shouldCreateUserAccount() {
        UserCreationRequest request = createUserCreationRequest();
        User user = createUser();
        UserShortDto expected = createUserShortDto();

        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toUserEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toUserShortDto(user)).thenReturn(expected);

        assertEquals(expected, userService.createUserAccount(request));

        verify(userRepository).existsByUsername(request.username());
        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("should reject account creation when username already exists")
    void shouldRejectAccountCreationWhenUsernameAlreadyExists() {
        UserCreationRequest request = createUserCreationRequest();
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.createUserAccount(request)
        );

        assertEquals("User with username nikita already exists", exception.getMessage());
        verify(userRepository, never()).existsByEmail(any());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should reject account creation when email already exists")
    void shouldRejectAccountCreationWhenEmailAlreadyExists() {
        UserCreationRequest request = createUserCreationRequest();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.createUserAccount(request)
        );

        assertEquals("User with email nikita@gmail.com already exists", exception.getMessage());
        verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("should save user to database")
    void shouldSaveUserToDatabase() {
        User user = createUser();
        when(userRepository.save(user)).thenReturn(user);

        assertSame(user, userService.saveToDatabase(user));

        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("should create OAuth2 user when username is available")
    void shouldCreateOAuth2UserWhenUsernameIsAvailable() {
        when(userRepository.existsByUsername("nikita")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createOAuth2User("nikita", "nikita@gmail.com");

        assertEquals("nikita", result.getUsername());
        assertEquals("nikita@gmail.com", result.getEmail());
        assertEquals(Set.of(UserRole.ROLE_USER), result.getRoles());
        assertNotNull(result.getRegisteredAt());

        verify(userRepository).existsByUsername("nikita");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should generate a unique username when OAuth2 username already exists")
    void shouldGenerateUniqueUsernameWhenOAuth2UsernameAlreadyExists() {
        when(userRepository.existsByUsername("nikita")).thenReturn(true);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createOAuth2User("nikita", "nikita@gmail.com");

        assertTrue(result.getUsername().matches("nikita_[a-f0-9]{5}"));
        assertEquals("nikita@gmail.com", result.getEmail());
        assertEquals(Set.of(UserRole.ROLE_USER), result.getRoles());
        assertNotNull(result.getRegisteredAt());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("should find user by username")
    void shouldFindUserByUsername() {
        User user = createUser();
        when(userRepository.findByUsernameOrEmail("nikita")).thenReturn(Optional.of(user));

        assertSame(user, userService.findByUsernameOrEmail("nikita"));
    }

    @Test
    @DisplayName("should find user by email")
    void shouldFindUserByEmail() {
        User user = createUser();
        when(userRepository.findByUsernameOrEmail("nikita@gmail.com")).thenReturn(Optional.of(user));

        assertSame(user, userService.findByUsernameOrEmail("nikita@gmail.com"));
    }

    @Test
    @DisplayName("should throw InvalidUserDataException when user is not found by username or email")
    void shouldThrowWhenUserIsNotFoundByUsernameOrEmail() {
        when(userRepository.findByUsernameOrEmail("unknown")).thenReturn(Optional.empty());

        InvalidUserDataException exception = assertThrows(
                InvalidUserDataException.class,
                () -> userService.findByUsernameOrEmail("unknown")
        );

        assertEquals("User not found with login/email: unknown", exception.getMessage());
    }

    @Test
    @DisplayName("should find user by email")
    void shouldFindByEmail() {
        User user = createUser();
        when(userRepository.findWithRolesByEmail("nikita@gmail.com")).thenReturn(Optional.of(user));

        assertEquals(Optional.of(user), userService.findByEmail("nikita@gmail.com"));
    }

    private User createUser() {
        return User.builder()
                .id(EXISTING_USER_ID)
                .username("nikita")
                .email("nikita@gmail.com")
                .password(NEW_PASSWORD)
                .roles(Set.of(UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.of(2026, 9, 10, 10, 10))
                .build();
    }

    private UserShortDto createUserShortDto() {
        return UserShortDto.builder()
                .id(EXISTING_USER_ID)
                .username("nikita")
                .email("nikita@gmail.com")
                .build();
    }

    private UserShortDto createUserShortDtoFor(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    private UserFullDto createFullDto(User user, String lastLogin) {
        return UserFullDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(List.of(UserRole.ROLE_USER.name()))
                .registeredAt("2026-09-10T10:10")
                .lastLogin(lastLogin)
                .linkedProviders(List.of())
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

    private ExternalIdentity identity(String provider, String subject, User user) {
        return new ExternalIdentity(provider, subject, user);
    }

    private UserSearchFilter emptySearchFilter() {
        return new UserSearchFilter(null, null, null, null, null, null, null);
    }
}
