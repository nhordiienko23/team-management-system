package com.nba.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // ==================== GET /api/users/me ====================

    @Test
    void shouldGetCurrentUserProfile() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        mockMvc.perform(
                        get("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userDetails.getUser().getId()))
                .andExpect(jsonPath("$.username").value("nikita_test"))
                .andExpect(jsonPath("$.email").value("nikita@gmail.com"));
    }

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("You need to log in to access this resource."));
    }

    // ==================== DELETE /api/users/me ====================

    @Test
    void shouldDeleteCurrentUserProfile() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        mockMvc.perform(
                        delete("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                )
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsByUsername(userDetails.getUsername()))
                .isFalse();
    }

    @Test
    void shouldReturnUnauthorizedWhenUserDeletingUnauthorizedUser() throws Exception {

        mockMvc.perform(
                        delete("/api/users/me")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("You need to log in to access this resource."));
    }

    // ==================== PATCH /api/users/me ====================

    @Test
    void shouldUpdateOnlyUsername() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("new_username")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_username"))
                .andExpect(jsonPath("$.email").value("nikita@gmail.com"));

        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(userDetails.getUser().getId());

        assertThat(userFromDatabase.getUsername())
                .isEqualTo("new_username");

        assertThat(userFromDatabase.getEmail())
                .isEqualTo("nikita@gmail.com");
    }

    @Test
    void shouldUpdateOnlyEmail() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("newEmail@gmail.com")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("nikita_test"))
                .andExpect(jsonPath("$.email").value("newEmail@gmail.com"));

        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(userDetails.getUser().getId());

        assertThat(userFromDatabase.getUsername())
                .isEqualTo("nikita_test");

        assertThat(userFromDatabase.getEmail())
                .isEqualTo("newEmail@gmail.com");
    }

    @Test
    void shouldUpdateUsernameAndEmail() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("newUsername")
                .email("newEmail@gmail.com")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newUsername"))
                .andExpect(jsonPath("$.email").value("newEmail@gmail.com"));

        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(userDetails.getUser().getId());

        assertThat(userFromDatabase.getUsername())
                .isEqualTo("newUsername");

        assertThat(userFromDatabase.getEmail())
                .isEqualTo("newEmail@gmail.com");
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("newUsername")
                .email("invalidEmail")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void shouldReturn400WhenUsernameAlreadyExists() throws Exception {

        createAndSaveUserToDatabase("nikita", "nikita@gmail.com");

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "n@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("nikita")
                .email("new@gmail.com")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with username nikita already exists"));
    }

    @Test
    void shouldReturn400WhenEmailAlreadyExists() throws Exception {

        createAndSaveUserToDatabase("nikita", "nikita@gmail.com");

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "n@gmail.com");

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("nikita_testNew")
                .email("nikita@gmail.com")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with email nikita@gmail.com already exists"));
    }

    @Test
    void shouldReturn401WhenUserUnauthenticated() throws Exception {

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("nikita_testNew")
                .email("nikita@gmail.com")
                .build();

        mockMvc.perform(
                        patch("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("You need to log in to access this resource."));
    }

    // ==================== PATCH /api/users/me/update-password ====================

    @Test
    void shouldUpdateCurrentUserPassword() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("password")
                .newPassword("newPassword")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Your password was successfully updated"));

        User userFromDatabase =
                userRepository.findUserByIdOrThrow404(userDetails.getUser().getId());

        assertThat(passwordEncoder.matches(
                "newPassword",
                userFromDatabase.getPassword()
        )).isTrue();
    }

    @Test
    void shouldReturn400WhenCurrentPasswordIsMissing() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .newPassword("newPassword")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Current password is required"));
    }

    @Test
    void shouldReturn400WhenCurrentPasswordIsWrong() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Current password is incorrect"));
    }

    @Test
    void shouldReturn400WhenNewPasswordIsTheSameAsCurrentPassword() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("password")
                .newPassword("password")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("The new password must be different from the current password"));
    }

    @Test
    void shouldReturn400WhenNewPasswordIsNotProvided() throws Exception {

        CustomUserDetails userDetails =
                createAuthenticatedUser("nikita_test", "nikita@gmail.com");

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("password")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Validation Failed"))
                .andExpect(jsonPath("$.message")
                        .value("New password is required"));
    }

    @Test
    void shouldReturn401WhenUserIsUnauthenticatedForPasswordUpdate() throws Exception {

        PasswordUpdateRequest request = PasswordUpdateRequest.builder()
                .currentPassword("password")
                .newPassword("newPassword")
                .build();

        mockMvc.perform(
                        patch("/api/users/me/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("You need to log in to access this resource."));
    }

    // ==================== Helpers ====================

    private CustomUserDetails createAuthenticatedUser(
            String username,
            String email
    ) {
        User user = createAndSaveUserToDatabase(username, email);
        return new CustomUserDetails(user);
    }

    private Authentication createAuthentication(CustomUserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private User createAndSaveUserToDatabase(String username, String email) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER))
                .registeredAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }
}