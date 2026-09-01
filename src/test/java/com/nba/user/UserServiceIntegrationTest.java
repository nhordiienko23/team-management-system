package com.nba.user;

import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.core.exception.notFound.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.*;


@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldCreateUserAccount() {
        UserCreationRequest request = createUserCreationRequest("nikita_test", "nikita@gmail.com");

        UserShortDto result = userService.createUserAccount(request);

        assertThat(result.id())
                .isNotNull();
        assertThat(result.username())
                .isEqualTo(request.username());
        assertThat(result.email())
                .isEqualTo(request.email());

        User savedUser = userRepository.findByUsername("nikita_test").orElseThrow();

        assertThat(savedUser.getEmail())
                .isEqualTo("nikita@gmail.com");

        assertThat(savedUser.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);

        assertThat(savedUser.getId())
                .isEqualTo(result.id());

        assertThat(savedUser.getUsername())
                .isEqualTo(request.username());

        assertThat(passwordEncoder.matches(
                request.password(),
                savedUser.getPassword()
        )).isTrue();

    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        UserCreationRequest firstRequest = createUserCreationRequest("nikita_test", "nikita@gmail.com");
        userService.createUserAccount(firstRequest);
        UserCreationRequest secondRequest = createUserCreationRequest("nikita_test", "n@gmail.com");


        assertThatThrownBy(() ->
                userService.createUserAccount(secondRequest))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage(
                        "User with username nikita_test already exists"
                );
    }


    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        UserCreationRequest firstRequest = createUserCreationRequest("nikita_test", "nikita@gmail.com");

        userService.createUserAccount(firstRequest);
        UserCreationRequest secondRequest = createUserCreationRequest("nikita", "nikita@gmail.com");

        assertThatThrownBy(() ->
                userService.createUserAccount(secondRequest))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage(
                        "User with email nikita@gmail.com already exists"
                );
    }

    @Test
    void shouldSaveUserAndReturnShortUserDto() {
        UserShortDto result = userService.saveToDataBaseAndReturnDto(
                createUser(
                        "nikita_test",
                        "nikita@gmail.com",
                        Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)
                ));
        User foundUser = userRepository.findByUsername("nikita_test").orElseThrow();

        assertThat(result.id())
                .isEqualTo(foundUser.getId());
        assertThat(result.username())
                .isEqualTo(foundUser.getUsername());
        assertThat(result.email())
                .isEqualTo(foundUser.getEmail());
    }

    @Test
    void shouldReturnUserProfileById() {
        User savedUser = userService.saveToDatabase(
                createUser(
                        "nikita_test",
                        "nikita@gmail.com",
                        Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)
                ));
        UserShortDto result = userService.getUserProfileById(savedUser.getId());

        assertThat(result.id())
                .isEqualTo(savedUser.getId());
        assertThat(result.email())
                .isEqualTo(savedUser.getEmail());
        assertThat(result.username())
                .isEqualTo(savedUser.getUsername());
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        assertThatThrownBy(() ->
                userService.getUserProfileById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");
    }

    @Test
    void shouldUpdateUsernameAndEmailByPartialUpdate() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "n@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        UserShortDto result = userService.partialUpdateUserProfileById(savedUser.getId(),
                UserUpdateRequest.builder()
                        .username("nikitaUpdate")
                        .email("n@github.com")
                        .build());


        assertThat(result.id())
                .isEqualTo(savedUser.getId());
        assertThat(result.username())
                .isEqualTo("nikitaUpdate");
        assertThat(result.email())
                .isEqualTo("n@github.com");

        User updatedUser = userRepository.findUserByIdOrThrow404(savedUser.getId());
        assertThat(updatedUser.getUsername())
                .isEqualTo("nikitaUpdate");
        assertThat(updatedUser.getEmail())
                .isEqualTo("n@github.com");
    }

    @Test
    void shouldThrowExceptionWhenNewUsernameAlreadyExists() {
        userService.saveToDatabase(createUser(
                "nikita",
                "n@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));
        User savedUser2 = userService.saveToDatabase(createUser(
                "n",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        assertThatThrownBy(() ->
                userService.partialUpdateUserProfileById(savedUser2.getId(),
                        UserUpdateRequest.builder()
                                .username("nikita")
                                .build()))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("User with username nikita already exists");
    }

    @Test
    void shouldThrowExceptionWhenNewEmailAlreadyExists() {
        userService.saveToDatabase(createUser(
                "nikita",
                "n@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));
        User savedUser2 = userService.saveToDatabase(createUser(
                "n",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        assertThatThrownBy(() ->
                userService.partialUpdateUserProfileById(savedUser2.getId(),
                        UserUpdateRequest.builder()
                                .email("n@gmail.com")
                                .build()))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("User with email n@gmail.com already exists");
    }

    @Test
    void shouldUpdateOnlyUsername() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        UserShortDto result = userService.partialUpdateUserProfileById(savedUser.getId(),
                UserUpdateRequest.builder()
                        .username("nikitaUpdate")
                        .build());

        assertThat(result.username())
                .isEqualTo("nikitaUpdate");

        assertThat(result.id())
                .isEqualTo(savedUser.getId());

        User updatedUser = userRepository.findUserByIdOrThrow404(savedUser.getId());
        assertThat(updatedUser.getUsername())
                .isEqualTo("nikitaUpdate");
        assertThat(updatedUser.getEmail())
                .isEqualTo("nikita@gmail.com");


    }

    @Test
    void shouldUpdateOnlyEmail() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        UserShortDto result = userService.partialUpdateUserProfileById(savedUser.getId(),
                UserUpdateRequest.builder()
                        .email("nikitaUpdate@gmail.com")
                        .build());


        assertThat(result.email())
                .isEqualTo("nikitaUpdate@gmail.com");

        assertThat(result.id())
                .isEqualTo(savedUser.getId());

        User updatedUser = userRepository.findUserByIdOrThrow404(savedUser.getId());

        assertThat(updatedUser.getEmail())
                .isEqualTo("nikitaUpdate@gmail.com");

        assertThat(updatedUser.getUsername())
                .isEqualTo("nikita");

    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        assertThatThrownBy(() ->
                userService.partialUpdateUserProfileById(999L,
                        UserUpdateRequest.builder()
                                .username("newUsername")
                                .build()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");
    }

    @Test
    void shouldKeepTheSameValue() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        UserShortDto result = userService.partialUpdateUserProfileById(savedUser.getId(),
                UserUpdateRequest.builder()
                        .username("nikita")
                        .email("nikita@gmail.com")
                        .build());


        assertThat(result.username())
                .isEqualTo("nikita");

        assertThat(result.email())
                .isEqualTo("nikita@gmail.com");

        assertThat(result.id())
                .isEqualTo(savedUser.getId());

        User updatedUser = userRepository.findUserByIdOrThrow404(savedUser.getId());
        assertThat(updatedUser.getUsername())
                .isEqualTo("nikita");
        assertThat(updatedUser.getEmail())
                .isEqualTo("nikita@gmail.com");

    }

    @Test
    void shouldThrowExceptionWhenUpdatingPasswordForNonExistingUser() {
        assertThatThrownBy(() ->
                userService.passwordUpdateByUserIdForCurrentUser(999L,
                        PasswordUpdateRequest.builder()
                                .currentPassword("password")
                                .newPassword("newPassword")
                                .build()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");
    }

    @Test
    void shouldThrowExceptionWhenCurrentPasswordNull() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));

        String oldPasswordHash = savedUser.getPassword();

        assertThatThrownBy(() -> userService.passwordUpdateByUserIdForCurrentUser(savedUser.getId(),
                PasswordUpdateRequest.builder()
                        .currentPassword(null)
                        .newPassword("newPassword")
                        .build()))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Current password is required");
        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(savedUser.getId());

        assertThat(userFromDatabase.getPassword())
                .isEqualTo(oldPasswordHash);
    }


    @Test
    void shouldThrowExceptionWhenCurrentPasswordWrong() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        String oldPasswordHash = savedUser.getPassword();
        assertThatThrownBy(() -> userService.passwordUpdateByUserIdForCurrentUser(savedUser.getId(),
                PasswordUpdateRequest.builder()
                        .currentPassword("wrongPass")
                        .newPassword("newPassword")
                        .build()))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("Current password is incorrect");

        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(savedUser.getId());

        assertThat(userFromDatabase.getPassword())
                .isEqualTo(oldPasswordHash);
    }

    @Test
    void shouldThrowExceptionWhenNewPasswordSame() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));

        String oldPasswordHash = savedUser.getPassword();

        assertThatThrownBy(() -> userService.passwordUpdateByUserIdForCurrentUser(savedUser.getId(),
                PasswordUpdateRequest.builder()
                        .currentPassword("password")
                        .newPassword("password")
                        .build()))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("The new password must be different from the current password");
        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(savedUser.getId());

        assertThat(userFromDatabase.getPassword())
                .isEqualTo(oldPasswordHash);
    }

    @Test
    void shouldUpdatePasswordWhenPasswordExists() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        userService.passwordUpdateByUserIdForCurrentUser(savedUser.getId(),
                PasswordUpdateRequest.builder()
                        .currentPassword("password")
                        .newPassword("newPassword")
                        .build());
        User userWithNewPassword = userRepository.findUserByIdOrThrow404(savedUser.getId());
        assertThat(passwordEncoder.matches("newPassword", userWithNewPassword.getPassword()))
                .isTrue();
    }

    @Test
    void shouldUpdatePasswordWhenPasswordDoesNotExist() {
        User user = createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER));
        user.setPassword(null);
        User savedUser = userService.saveToDatabase(user);
        userService.passwordUpdateByUserIdForCurrentUser(savedUser.getId(),
                PasswordUpdateRequest.builder()
                        .newPassword("newPassword")
                        .build());
        User userWithNewPassword = userRepository.findUserByIdOrThrow404(savedUser.getId());
        assertThat(passwordEncoder.matches("newPassword", userWithNewPassword.getPassword()))
                .isTrue();
    }

    @Test
    void shouldReturnUserByUsername() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));

        User userFromDatabase = userService.getUserByUsername("nikita");
        assertThat(userFromDatabase.getId())
                .isEqualTo(savedUser.getId());
        assertThat(userFromDatabase.getUsername())
                .isEqualTo("nikita");
        assertThat(userFromDatabase.getEmail())
                .isEqualTo("nikita@gmail.com");
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByUsername() {
        assertThatThrownBy(() -> userService.getUserByUsername("nikita"))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("User with username nikita not found");
    }

    @Test
    void shouldSetPasswordById() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        userService.setUserPasswordById(savedUser.getId(), "newPassword");

        User userFromDatabase = userService.getUserByUsername("nikita");

        assertThat(passwordEncoder.matches(
                "newPassword",
                userFromDatabase.getPassword()))
                .isTrue();

        assertThat(passwordEncoder.matches(
                "password",
                userFromDatabase.getPassword()))
                .isFalse();
    }

    @Test
    void shouldThrowExceptionWhenSettingPasswordForNonExistingUser() {
        assertThatThrownBy(() -> userService.setUserPasswordById(
                999L,
                "newPassword"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");

    }

    @Test
    void shouldDeleteUserById() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        assertThat(userRepository.existsByUsername("nikita"))
                .isTrue();
        userService.deleteUserById(savedUser.getId());
        assertThat(userRepository.existsByUsername("nikita"))
                .isFalse();
        assertThat(userRepository.existsByEmail("nikita@gmail.com"))
                .isFalse();
        Optional<User> userFromDatabase = userRepository.findById(savedUser.getId());
        assertThat(userFromDatabase).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingUser() {
        assertThatThrownBy(() -> userService.deleteUserById(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 999 not found");
    }

    @Test
    void shouldReturnPageOfAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserShortDto> usersPageBeforeAction = userService.getAllUsers(pageable);
        int amountOfUsersBeforeAction = usersPageBeforeAction.getContent().size();
        User nikita = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));
        User sveta = userService.saveToDatabase(createUser(
                "sveta",
                "sveta@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));

        Page<UserShortDto> usersPage = userService.getAllUsers(pageable);

        assertThat(usersPage.getTotalElements())
                .isEqualTo(amountOfUsersBeforeAction + 2);

        assertThat(usersPage.getContent())
                .extracting(UserShortDto::username)
                .contains("nikita", "sveta");
    }

    @Test
    void shouldReturnUsersMatchingAllSearchFilters() {
        User nikita = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));
        User sveta = userService.saveToDatabase(createUser(
                "sveta",
                "sveta@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)));
        UserSearchFilter filter = new UserSearchFilter(
                "T",
                "gmail",
                Set.of(UserRole.ROLE_USER),
                LocalDate.of(2026, 8, 31),
                null,
                null,
                null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserFullDto> usersPage = userService.searchUsers(filter, pageable);
        assertThat(usersPage.getContent())
                .hasSize(2);
        assertThat(usersPage.getContent())
                .extracting(UserFullDto::username)
                .contains("nikita", "sveta");

        assertThat(usersPage.getContent())
                .extracting(UserFullDto::email)
                .contains("nikita@gmail.com", "sveta@gmail.com");

        assertThat(usersPage.getContent())
                .allSatisfy(user -> {
                    assertThat(user.username()).isNotNull();
                    assertThat(user.email()).isNotNull();
                });
    }

    @Test
    void shouldCreateNewUserAndSaveToDatabase() {
        User savedUser = userService.createOAuth2User(
                "nikita",
                "nikita@gmail.com");

        User userFromDatabase = userRepository.findUserByIdOrThrow404(savedUser.getId());

        assertThat(userFromDatabase.getUsername())
                .isEqualTo("nikita");
        assertThat(userFromDatabase.getEmail())
                .isEqualTo("nikita@gmail.com");
        assertThat(userFromDatabase.getRoles())
                .isEqualTo(Set.of(UserRole.ROLE_USER));

    }

    @Test
    void shouldCreateNewUserWhenUsernameAlreadyExistAndSaveToDatabase() {
        userService.createOAuth2User(
                "nikita",
                "nikita@gmail.com");

        userService.createOAuth2User(
                "nikita",
                "n@gmail.com");

        User userFromDatabase = userService.findByUsernameOrEmail("n@gmail.com");


        assertThat(userFromDatabase.getEmail())
                .isEqualTo("n@gmail.com");
        assertThat(userFromDatabase.getRoles())
                .isEqualTo(Set.of(UserRole.ROLE_USER));
        assertThat(userFromDatabase.getUsername())
                .startsWith("nikita_");

        assertThat(userFromDatabase.getUsername())
                .hasSize(12);

    }

    @Test
    void shouldFindUserByUsernameOrEmail() {
        User savedUser = userService.saveToDatabase(createUser(
                "nikita",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER)));

        User byUsername =
                userService.findByUsernameOrEmail("nikita");

        User byEmail =
                userService.findByUsernameOrEmail("nikita@gmail.com");

        assertThat(byUsername.getId())
                .isEqualTo(savedUser.getId());

        assertThat(byEmail.getId())
                .isEqualTo(savedUser.getId());
    }

    @Test
    void shouldThrowExceptionWhenUsernameOrEmailNotFound() {
        assertThatThrownBy(() ->
                userService.findByUsernameOrEmail("unknown"))
                .isInstanceOf(InvalidUserDataException.class)
                .hasMessage("User not found with login/email: unknown");
    }


    private User createUser(String username, String email, Set<UserRole> roles) {
        return User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .registeredAt(LocalDateTime.of(2026, 8, 31, 10, 10))
                .roles(roles)
                .lastLogin(LocalDateTime.of(2026, 8, 31, 10, 10))
                .build();
    }


    private UserCreationRequest createUserCreationRequest(String username, String email) {
        return UserCreationRequest.builder()
                .username(username)
                .password("password")
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN))
                .build();
    }


}
