package com.nba.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class AdminUserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalIdentityRepository externalIdentityRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // ============================================================
    // GET /api/admin/users
    // ============================================================

    @Test
    void shouldReturnPaginatedUsersForAdmin() throws Exception {
        int totalSizeBeforeAction = userRepository.findAll().size();

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "nikita_test",
                "nikita@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN));

        createAuthenticatedUser(
                "sveta_test",
                "sveta@gmail.com",
                Set.of(UserRole.ROLE_USER));

        createAuthenticatedUser(
                "alex_test",
                "alex@gmail.com",
                Set.of(UserRole.ROLE_USER));

        String countPerPage = String.valueOf(totalSizeBeforeAction + 2);

        mockMvc.perform(
                        get("/api/admin/users")
                                .param("page", "0")
                                .param("size", countPerPage)
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()")
                        .value(Integer.valueOf(countPerPage)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size")
                        .value(Integer.valueOf(countPerPage)));
    }


    @Test
    void shouldReturnForbiddenForRegularUser() throws Exception {

        CustomUserDetails userDetails = createAuthenticatedUser(
                "regular_user",
                "regular_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users")
                                .with(authentication(createAuthentication(userDetails)))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldForbidRegularUserFromSearchingViewingAndUpdatingUsers() throws Exception {
        CustomUserDetails regularUser = createAuthenticatedUser(
                "regular_user_admin_routes",
                "regular_user_admin_routes@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );
        User targetUser = createAndSaveUserToDatabase(
                "admin_route_target",
                "admin_route_target@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .with(authentication(createAuthentication(regularUser)))
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/admin/users/{userId}", targetUser.getId())
                                .with(authentication(createAuthentication(regularUser)))
                )
                .andExpect(status().isForbidden());

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .username("unauthorized_update")
                .build();
        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", targetUser.getId())
                                .with(csrf())
                                .with(authentication(createAuthentication(regularUser)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest))
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // GET /api/admin/users/search
    // ============================================================

    @Test
    void shouldReturnUsersWithoutSearchFiltersForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "search_admin",
                "search_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAuthenticatedUser(
                "search_user_1",
                "search1@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAuthenticatedUser(
                "search_user_2",
                "search2@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].roles").isArray())
                .andExpect(jsonPath("$.content[0].registeredAt").exists())
                .andExpect(jsonPath("$.content[0].linkedProviders").isArray())
                .andExpect(jsonPath("$.content[*].username")
                        .value(hasItem("search_user_1")))
                .andExpect(jsonPath("$.content[*].username")
                        .value(hasItem("search_user_2")));
    }


    @Test
    void shouldReturnUsersMatchingUsernameFilter() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "username_filter_admin",
                "username_filter_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAuthenticatedUser(
                "special_search_user",
                "special1@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAuthenticatedUser(
                "another_search_user",
                "another@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .param("username", "special_search_user")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].username")
                        .value(hasItem("special_search_user")));
    }


    @Test
    void shouldReturnUsersMatchingAllSearchFilters() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "multiple_filter_admin",
                "multiple_filter_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAuthenticatedUser(
                "filter_target",
                "target_filter@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAuthenticatedUser(
                "another_filter_target",
                "another_filter@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .param("username", "filter_target")
                                .param("email", "target_filter@gmail.com")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username")
                        .value("filter_target"))
                .andExpect(jsonPath("$.content[0].email")
                        .value("target_filter@gmail.com"));
    }


    @Test
    void shouldReturnFilteredUsersWithPagination() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "search_pagination_admin",
                "search_pagination_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAuthenticatedUser(
                "paginated_search_1",
                "paginated_search_1@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAuthenticatedUser(
                "paginated_search_2",
                "paginated_search_2@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAuthenticatedUser(
                "paginated_search_3",
                "paginated_search_3@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .param("username", "paginated_search")
                                .param("page", "0")
                                .param("size", "2")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }


    // ============================================================
    // GET /api/admin/users/{userId}
    // ============================================================

    @Test
    void shouldReturnUserByIdForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "get_user_admin",
                "get_user_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User user = createAndSaveUserToDatabase(
                "target_user",
                "target_user@gmail.com",
                Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN)
        );
        user.setRegisteredAt(LocalDateTime.of(2026, 9, 10, 10, 10));
        user.setLastLogin(null);
        externalIdentityRepository.save(new ExternalIdentity(
                "https://accounts.google.com", "google-private-subject", user));
        externalIdentityRepository.save(new ExternalIdentity(
                "github", "github-private-subject", user));
        externalIdentityRepository.save(new ExternalIdentity(
                "http://localhost:9000", "custom-private-subject", user));
        externalIdentityRepository.save(new ExternalIdentity(
                "https://unknown.example/issuer", "unknown-private-subject", user));

        mockMvc.perform(
                        get("/api/admin/users/{userId}", user.getId())
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("target_user"))
                .andExpect(jsonPath("$.email").value("target_user@gmail.com"))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.registeredAt").value("2026-09-10T10:10"))
                .andExpect(jsonPath("$.lastLoginAt").value(nullValue()))
                .andExpect(jsonPath("$.linkedProviders.length()").value(4))
                .andExpect(jsonPath("$.linkedProviders", hasItem("Google")))
                .andExpect(jsonPath("$.linkedProviders", hasItem("GitHub")))
                .andExpect(jsonPath("$.linkedProviders", hasItem("Custom authorization server")))
                .andExpect(jsonPath("$.linkedProviders", hasItem("Other sign-in provider")))
                .andExpect(jsonPath("$.subject").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

        mockMvc.perform(
                        get("/api/admin/users/search")
                                .param("username", "target_user")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.content[0].linkedProviders.length()").value(4))
                .andExpect(jsonPath("$.content[0].linkedProviders", hasItem("Google")));
    }


    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "not_found_admin",
                "not_found_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        long nonExistingUserId = 999999L;

        mockMvc.perform(
                        get("/api/admin/users/{userId}", nonExistingUserId)
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenUserIdIsInvalid() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "invalid_id_admin",
                "invalid_id_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        mockMvc.perform(
                        get("/api/admin/users/{userId}", "abc")
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isBadRequest());
    }


    // ============================================================
    // PATCH /api/admin/users/{userId}
    // ============================================================

    @Test
    void shouldUpdateUserProfileByIdForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "update_admin",
                "update_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User user = createAndSaveUserToDatabase(
                "old_username",
                "old_email@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("new_username")
                .email("new_email@gmail.com")
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", user.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("new_username"))
                .andExpect(jsonPath("$.email").value("new_email@gmail.com"));

        User updatedUser = userRepository.findById(user.getId())
                .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(updatedUser.getUsername())
                .isEqualTo("new_username");

        org.assertj.core.api.Assertions.assertThat(updatedUser.getEmail())
                .isEqualTo("new_email@gmail.com");
    }


    @Test
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "invalid_email_admin",
                "invalid_email_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User user = createAndSaveUserToDatabase(
                "email_target_user",
                "email_target@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("invalid-email")
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", user.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Please enter a valid email address, like name@example.com."
                ));
    }

    @Test
    void shouldRejectBlankEmailWhenItIsSuppliedInPatch() throws Exception {
        CustomUserDetails adminDetails = createAuthenticatedUser(
                "blank_patch_email_admin",
                "blank_patch_email_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );
        User user = createAndSaveUserToDatabase(
                "blank_patch_email_user",
                "blank_patch_email_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("")
                .build();

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", user.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter your email address."));

        org.assertj.core.api.Assertions.assertThat(userRepository.findById(user.getId()).orElseThrow().getEmail())
                .isEqualTo("blank_patch_email_user@gmail.com");
    }


    @Test
    void shouldAllowPatchWhenEmailIsOmitted() throws Exception {
        CustomUserDetails adminDetails = createAuthenticatedUser(
                "patch_without_email_admin",
                "patch_without_email_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );
        User user = createAndSaveUserToDatabase(
                "patch_without_email_user",
                "patch_without_email_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("patch_without_email_updated")
                .build();

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", user.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("patch_without_email_updated"))
                .andExpect(jsonPath("$.email").value("patch_without_email_user@gmail.com"));
    }


    @Test
    void shouldReturnBadRequestWhenUsernameAlreadyExists() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "duplicate_username_admin",
                "duplicate_username_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User targetUser = createAndSaveUserToDatabase(
                "target_update_user",
                "target_update@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAndSaveUserToDatabase(
                "existing_username",
                "existing_username@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("existing_username")
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "duplicate_email_admin",
                "duplicate_email_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User targetUser = createAndSaveUserToDatabase(
                "target_email_user",
                "target_email_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        createAndSaveUserToDatabase(
                "existing_email_user",
                "existing_email@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("existing_email@gmail.com")
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest());
    }


    @Test
    void shouldReturnNotFoundWhenUpdatingNonExistingUser() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "update_not_found_admin",
                "update_not_found_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        long nonExistingUserId = 999999L;

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("new_username")
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}", nonExistingUserId).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound());
    }


    // ============================================================
    // DELETE /api/admin/users/{userId}
    // ============================================================

    @Test
    void shouldDeleteUserByIdForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "delete_admin",
                "delete_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User targetUser = createAndSaveUserToDatabase(
                "user_to_delete",
                "user_to_delete@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/admin/users/{userId}", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Profile of user with id " + targetUser.getId()
                                + " was successfully deleted."));

        org.assertj.core.api.Assertions.assertThat(
                        userRepository.findById(targetUser.getId())
                )
                .isEmpty();
    }


    @Test
    void shouldReturnBadRequestWhenAdminTriesToDeleteOwnAccount() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "self_delete_admin",
                "self_delete_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        Long adminId = adminDetails.getUser().getId();

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/admin/users/{userId}", adminId).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Admin cannot delete their own account using this endpoint"));

        org.assertj.core.api.Assertions.assertThat(
                        userRepository.findById(adminId)
                )
                .isPresent();
    }


    @Test
    void shouldReturnNotFoundWhenDeletingNonExistingUser() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "delete_not_found_admin",
                "delete_not_found_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        long nonExistingUserId = 999999L;

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/admin/users/{userId}", nonExistingUserId).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToDeleteUser() throws Exception {

        CustomUserDetails userDetails = createAuthenticatedUser(
                "delete_regular_user",
                "delete_regular_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        User targetUser = createAndSaveUserToDatabase(
                "delete_target_user",
                "delete_target_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/admin/users/{userId}", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(userDetails)))
                )
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(
                        userRepository.findById(targetUser.getId())
                )
                .isPresent();
    }


    // ============================================================
    // POST /api/admin/users
    // ============================================================

    @Test
    void shouldCreateNewUserForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "create_admin",
                "create_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        UserCreationRequest request = UserCreationRequest.builder()
                .username("new_user")
                .password("password123")
                .email("new_user@gmail.com")
                .roles(Set.of(UserRole.ROLE_USER, UserRole.ROLE_ADMIN))
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/users").with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.email").value("new_user@gmail.com"));

        User createdUser = userRepository.findByUsername("new_user")
                .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(createdUser.getEmail())
                .isEqualTo("new_user@gmail.com");

        org.assertj.core.api.Assertions.assertThat(createdUser.getRoles())
                .containsExactlyInAnyOrder(UserRole.ROLE_USER, UserRole.ROLE_ADMIN);

        org.assertj.core.api.Assertions.assertThat(
                        passwordEncoder.matches(
                                "password123",
                                createdUser.getPassword()
                        )
                )
                .isTrue();
    }


    @Test
    void shouldReturnBadRequestWhenCreatingUserWithExistingUsername() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "create_duplicate_username_admin",
                "create_duplicate_username_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAndSaveUserToDatabase(
                "existing_create_username",
                "existing_create_username@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserCreationRequest request = UserCreationRequest.builder()
                .username("existing_create_username")
                .password("password123")
                .email("new_create_email@gmail.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/users").with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with username existing_create_username already exists"));
    }


    @Test
    void shouldReturnBadRequestWhenCreatingUserWithExistingEmail() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "create_duplicate_email_admin",
                "create_duplicate_email_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        createAndSaveUserToDatabase(
                "existing_create_email",
                "existing_create_email@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserCreationRequest request = UserCreationRequest.builder()
                .username("new_create_username")
                .password("password123")
                .email("existing_create_email@gmail.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/users").with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with email existing_create_email@gmail.com already exists"));
    }


    @Test
    void shouldReturnBadRequestWhenCreatingUserWithInvalidData() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "create_validation_admin",
                "create_validation_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        UserCreationRequest request = UserCreationRequest.builder()
                .username("")
                .password("")
                .email("invalid-email")
                .roles(Set.of())
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/users").with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Please enter your username."),
                        org.hamcrest.Matchers.containsString("Please enter your password."),
                        org.hamcrest.Matchers.containsString("Please enter a valid email address, like name@example.com."),
                        org.hamcrest.Matchers.containsString("Please select at least one role.")
                )));
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToCreateUser() throws Exception {

        CustomUserDetails userDetails = createAuthenticatedUser(
                "create_regular_user",
                "create_regular_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        UserCreationRequest request = UserCreationRequest.builder()
                .username("new_regular_created_user")
                .password("password123")
                .email("new_regular_created_user@gmail.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/admin/users").with(csrf())
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // PATCH /api/admin/users/{userId}/updateUserPassword
    // ============================================================

    @Test
    void shouldSetUserPasswordForAdmin() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "password_admin",
                "password_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User targetUser = createAndSaveUserToDatabase(
                "password_target_user",
                "password_target_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        PasswordUpdateRequestForAdmins request =
                new PasswordUpdateRequestForAdmins("newPassword123");

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}/updateUserPassword", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("New password was successfully set for user with id "
                                + targetUser.getId()));

        User updatedUser = userRepository.findById(targetUser.getId())
                .orElseThrow();

        org.assertj.core.api.Assertions.assertThat(
                        passwordEncoder.matches(
                                "newPassword123",
                                updatedUser.getPassword()
                        )
                )
                .isTrue();
    }


    @Test
    void shouldReturnNotFoundWhenSettingPasswordForNonExistingUser() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "password_not_found_admin",
                "password_not_found_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        long nonExistingUserId = 999999L;

        PasswordUpdateRequestForAdmins request =
                new PasswordUpdateRequestForAdmins("newPassword123");

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}/updateUserPassword", nonExistingUserId).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isNotFound());
    }


    @Test
    void shouldReturnBadRequestWhenNewPasswordIsInvalid() throws Exception {

        CustomUserDetails adminDetails = createAuthenticatedUser(
                "password_validation_admin",
                "password_validation_admin@gmail.com",
                Set.of(UserRole.ROLE_ADMIN)
        );

        User targetUser = createAndSaveUserToDatabase(
                "password_validation_user",
                "password_validation_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        PasswordUpdateRequestForAdmins request =
                new PasswordUpdateRequestForAdmins("");

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}/updateUserPassword", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(adminDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("New password is required"));
    }


    @Test
    void shouldReturnForbiddenWhenRegularUserTriesToSetUserPassword() throws Exception {

        CustomUserDetails userDetails = createAuthenticatedUser(
                "password_regular_user",
                "password_regular_user@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        User targetUser = createAndSaveUserToDatabase(
                "password_target_regular",
                "password_target_regular@gmail.com",
                Set.of(UserRole.ROLE_USER)
        );

        PasswordUpdateRequestForAdmins request =
                new PasswordUpdateRequestForAdmins("newPassword123");

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/admin/users/{userId}/updateUserPassword", targetUser.getId()).with(csrf())
                                .with(authentication(createAuthentication(userDetails)))
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden());
    }


    // ============================================================
    // Helpers
    // ============================================================

    private User createAndSaveUserToDatabase(
            String username,
            String email,
            Set<UserRole> roles
    ) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("password"))
                .email(email)
                .roles(roles)
                .registeredAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }


    private CustomUserDetails createAuthenticatedUser(
            String username,
            String email,
            Set<UserRole> roles
    ) {
        User user = createAndSaveUserToDatabase(
                username,
                email,
                roles
        );

        return new CustomUserDetails(user);
    }


    private Authentication createAuthentication(
            CustomUserDetails userDetails
    ) {
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}