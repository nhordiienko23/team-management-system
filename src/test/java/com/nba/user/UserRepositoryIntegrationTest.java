package com.nba.user;

import com.nba.core.exception.notFound.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryIntegrationTest {
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16").withDatabaseName("test_db").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturnUserByUsernameWithRoles() {
        create1UserAndSaveToDatabase();
        User foundUser = userRepository.findByUsername("nikita_test").orElseThrow();
        assertThat(foundUser.getUsername())
                .isEqualTo("nikita_test");
        assertThat(foundUser.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
    }

    @Test
    void shouldReturnUserWithRolesById() {
        User savedUser = create1UserAndSaveToDatabase();
        User foundUser = userRepository.findWithRolesById(savedUser.getId()).orElseThrow();
        assertThat(foundUser.getId())
                .isEqualTo(savedUser.getId());
        assertThat(foundUser.getUsername())
                .isEqualTo("nikita_test");
        assertThat(foundUser.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
    }

    @Test
    void shouldFindWithRolesByEmail() {
        create1UserAndSaveToDatabase();
        User foundUser = userRepository.findWithRolesByEmail("n@gmail.com").orElseThrow();
        assertThat(foundUser.getEmail())
                .isEqualTo("n@gmail.com");
        assertThat(foundUser.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
    }

    @Test
    void shouldFindByUsernameOrEmail() {
        create1UserAndSaveToDatabase();
        Optional<User> foundUserByUsername = userRepository.findByUsernameOrEmail("nikita_test");
        Optional<User> foundUserByEmail = userRepository.findByUsernameOrEmail("n@gmail.com");
        assertThat(foundUserByUsername).isPresent();
        assertThat(foundUserByEmail).isPresent();
        assertThat(foundUserByUsername.orElseThrow().getId())
                .isEqualTo(foundUserByEmail.orElseThrow().getId());
        assertThat(foundUserByUsername.orElseThrow().getUsername())
                .isEqualTo(foundUserByEmail.orElseThrow().getUsername());
        assertThat(foundUserByUsername.orElseThrow().getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
    }

    @Test
    void shouldReturnEmptyWhenUsernameOrEmailDoesNotExist() {
        Optional<User> foundUser = userRepository.findByUsernameOrEmail("unknown");
        assertThat(foundUser).isEmpty();
    }

    @Test
    void shouldThrowUserNotFoundExceptionWhenUserDoesNotExist() {
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> userRepository.findUserByIdOrThrow404(444L));
        assertEquals("User with id 444 not found", exception.getMessage());
    }

    @Test
    void shouldFindAllWithRoles() {
        create3UsersAndSaveToDatabase();
        List<User> foundUsers = userRepository.findAllWithRoles();
        assertThat(foundUsers).extracting(User::getUsername).contains("nikita_test", "sveta_test", "yuri_test");
        User foundNikita = findUserByUsername(foundUsers, "nikita_test");
        User foundSveta = findUserByUsername(foundUsers, "sveta_test");
        User foundYuri = findUserByUsername(foundUsers, "yuri_test");
        assertThat(foundNikita.getRoles())
                .containsExactly(UserRole.ROLE_USER);
        assertThat(foundSveta.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);
        assertThat(foundYuri.getRoles())
                .containsExactly(UserRole.ROLE_ADMIN);
    }

    @Test
    void shouldFindUsersByUsernameIgnoreCase() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter("SVE", null, null, null, null, null, null);
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers).hasSize(1);
        assertThat(foundUsers.get(0).getUsername())
                .isEqualTo("sveta_test");
    }

    @Test
    void shouldFindUsersByEmail() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter(null, "@gmail.com", null, null, null, null, null);
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers)
                .allSatisfy(user ->
                        assertThat(user.getEmail()).contains("@gmail.com"));
        assertThat(foundUsers)
                .extracting(User::getEmail)
                .contains("n@gmail.com", "s@gmail.com", "y@gmail.com");
    }

    @Test
    void shouldFindUsersByRole() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter(null, null, Set.of(UserRole.ROLE_ADMIN), null, null, null, null);
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers)
                .extracting(User::getUsername)
                .contains("sveta_test", "yuri_test")
                .doesNotContain("nikita_test");
        assertThat(foundUsers)
                .allSatisfy(user ->
                        assertThat(user.getRoles())
                                .contains(UserRole.ROLE_ADMIN));
    }

    @Test
    void shouldReturnUsersByRegisteredAt() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter(null, null, null, LocalDate.of(2026, 8, 20), null, null, null);
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers)
                .extracting(User::getUsername)
                .contains("sveta_test", "yuri_test")
                .doesNotContain("nikita_test");
        LocalDateTime start = LocalDate.of(2026, 8, 20).atStartOfDay();
        assertThat(foundUsers)
                .allSatisfy(user ->
                        assertThat(user.getRegisteredAt())
                                .isAfterOrEqualTo(start));
    }

    @Test
    void shouldReturnUsersByLastLoginDate() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter(null, null, null, null, null, LocalDate.of(2026, 8, 20), null);
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers)
                .extracting(User::getUsername)
                .contains("sveta_test", "yuri_test")
                .doesNotContain("nikita_test");
        LocalDateTime start = LocalDate.of(2026, 8, 20).atStartOfDay();
        assertThat(foundUsers)
                .allSatisfy(user ->
                        assertThat(user.getLastLogin())
                                .isAfterOrEqualTo(start));
    }

    @Test
    void shouldReturnUsersByMultipleFilters() {
        create3UsersAndSaveToDatabase();
        UserSearchFilter filter = new UserSearchFilter("A", "gmail", Set.of(UserRole.ROLE_USER), LocalDate.of(2026, 8, 1), null, null, LocalDate.of(2026, 8, 31));
        List<User> foundUsers = userRepository.findAll(UserSpecification.buildQuery(filter));
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers)
                .extracting(User::getUsername)
                .contains("nikita_test", "sveta_test").doesNotContain("yuri_test");
        LocalDateTime registeredAtStart = LocalDate.of(2026, 8, 1).atStartOfDay();
        LocalDateTime lastLoginEnd = LocalDate.of(2026, 8, 31).atTime(LocalTime.MAX);
        assertThat(foundUsers)
                .allSatisfy(user -> {
                    assertThat(user.getRegisteredAt())
                            .isAfterOrEqualTo(registeredAtStart);
                    assertThat(user.getLastLogin())
                            .isBeforeOrEqualTo(lastLoginEnd);
                });
    }

    private User create1UserAndSaveToDatabase() {
        User user = createUser(
                "nikita_test",
                "n@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN));
        return userRepository.save(user);
    }

    private void create3UsersAndSaveToDatabase() {
        User nikita = createUser(
                "nikita_test",
                "n@gmail.com",
                Set.of(UserRole.ROLE_USER));
        User sveta = createUser(
                "sveta_test",
                "s@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN));
        User yuri = createUser(
                "yuri_test",
                "y@gmail.com",
                Set.of(UserRole.ROLE_ADMIN));
        nikita.setRegisteredAt(LocalDateTime.of(2026, 8, 10, 10, 10));
        sveta.setRegisteredAt(LocalDateTime.of(2026, 8, 20, 10, 10));
        yuri.setRegisteredAt(LocalDateTime.of(2026, 8, 30, 10, 10));
        nikita.setLastLogin(LocalDateTime.of(2026, 8, 10, 10, 10));
        sveta.setLastLogin(LocalDateTime.of(2026, 8, 20, 10, 10));
        yuri.setLastLogin(LocalDateTime.of(2026, 8, 30, 10, 10));
        userRepository.saveAll(List.of(nikita, sveta, yuri));
    }

    private User findUserByUsername(List<User> users, String username) {
        return users.stream().
                filter(user -> user.getUsername().equals(username)).
                findFirst().
                orElseThrow();
    }

    private User createUser(String username, String email, Set<UserRole> roles) {
        return User.builder()
                .username(username)
                .password("password")
                .email(email)
                .roles(roles)
                .registeredAt(LocalDateTime.now())
                .build();
    }
}