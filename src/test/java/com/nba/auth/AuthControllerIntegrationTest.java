package com.nba.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.AbstractIntegrationTest;
import com.nba.security.CustomUserDetails;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import com.nba.user.UserUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FilterChainProxy securityFilterChain;

    @Autowired
    private Environment environment;

    @Test
    void shouldReturnCurrentIdentityAndAuthorities() throws Exception {
        User user = createUser("identity_user", "identity@example.com", UserRole.ROLE_USER);

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(new UsernamePasswordAuthenticationToken(
                                new CustomUserDetails(user), null,
                                new CustomUserDetails(user).getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("identity_user"))
                .andExpect(jsonPath("$.email").value("identity@example.com"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn401ForAnonymousIdentityRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldExposeAnUncachedCsrfTokenForAnonymousAndAuthenticatedSessions() throws Exception {
        CsrfSession anonymousCsrf = fetchCsrf();

        assertThat(anonymousCsrf.token().token()).isNotBlank();
        assertThat(anonymousCsrf.token().parameterName()).isNotBlank();
        assertThat(anonymousCsrf.token().headerName()).isNotBlank();

        User user = createUser("csrf_user", "csrf@example.com", UserRole.ROLE_USER);
        MvcResult authenticatedResult = mockMvc.perform(get("/api/auth/csrf")
                        .session(anonymousCsrf.session())
                        .with(authentication(authenticationFor(user))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.parameterName").isNotEmpty())
                .andExpect(jsonPath("$.headerName").isNotEmpty())
                .andReturn();

        assertThat(authenticatedResult.getRequest().getSession(false)).isNotNull();
        assertThat(anonymousCsrf.token().token()).isNotEqualTo(
                objectMapper.readValue(
                        authenticatedResult.getResponse().getContentAsString(),
                        AuthController.CsrfTokenResponse.class
                ).token()
        );
    }

    @Test
    void shouldRejectMissingAndInvalidCsrfTokensForLogin() throws Exception {
        createUser("csrf_login", "csrf-login@example.com", UserRole.ROLE_USER);

        mockMvc.perform(post("/login")
                        .param("username", "csrf_login")
                        .param("password", "correct-password"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?csrfError"));

        CsrfSession csrf = fetchCsrf();
        mockMvc.perform(post("/login")
                        .session(csrf.session())
                        .param("username", "csrf_login")
                        .param("password", "correct-password")
                        .param(csrf.token().parameterName(), "invalid-token"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?csrfError"));
    }

    @Test
    void shouldRedirectSuccessfulFormLoginToFrontendRoot() throws Exception {
        User user = createUser("form_user", "form@example.com", UserRole.ROLE_USER);
        CsrfSession csrf = fetchCsrf();
        String oldSessionId = csrf.session().getId();

        MvcResult loginResult = mockMvc.perform(post("/login")
                        .session(csrf.session())
                        .param("username", "form_user")
                        .param("password", "correct-password")
                        .param(csrf.token().parameterName(), csrf.token().token()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/"))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(authenticatedSession).isNotNull();
        assertThat(authenticatedSession.getId()).isNotEqualTo(oldSessionId);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getLastLogin())
                .isNotNull();

        mockMvc.perform(post("/api/auth/register")
                        .session(authenticatedSession)
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration("after_login", "after-login@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF Failed"));

        CsrfSession freshCsrf = fetchCsrf(authenticatedSession);
        mockMvc.perform(get("/api/auth/me").session(freshCsrf.session()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRedirectSuccessfulOAuthLoginToFrontendRoot() throws Exception {
        OAuth2LoginAuthenticationFilter oauthFilter = securityFilterChain.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast)
                .findFirst()
                .orElseThrow();
        AuthenticationSuccessHandler successHandler = (AuthenticationSuccessHandler)
                ReflectionTestUtils.getField(oauthFilter, "successHandler");
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new TestingAuthenticationToken("oauth-user", null)
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/");
    }

    @Test
    void shouldRedirectFailedOAuthLoginToGenericFrontendError() throws Exception {
        OAuth2LoginAuthenticationFilter oauthFilter = securityFilterChain.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(OAuth2LoginAuthenticationFilter.class::cast)
                .findFirst()
                .orElseThrow();
        AuthenticationFailureHandler failureHandler = (AuthenticationFailureHandler)
                ReflectionTestUtils.getField(oauthFilter, "failureHandler");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("authentication_failed"));

        failureHandler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                exception
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/sign-in?error");
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "github", "my-custom-auth"})
    void shouldKeepOAuthCallbackAndStateValidation(String provider) throws Exception {
        MvcResult authorizationResult = mockMvc.perform(get("/oauth2/authorization/" + provider)
                        .with(request -> {
                            request.setServerName("localhost");
                            request.setServerPort(8080);
                            return request;
                        }))
                .andExpect(status().isFound())
                .andReturn();

        String location = authorizationResult.getResponse().getHeader("Location");
        String state = UriComponentsBuilder.fromUriString(location)
                .build()
                .getQueryParams()
                .getFirst("state");
        String redirectUri = UriComponentsBuilder.fromUriString(location)
                .build()
                .getQueryParams()
                .getFirst("redirect_uri");
        MockHttpSession oauthSession =
                (MockHttpSession) authorizationResult.getRequest().getSession(false);

        assertThat(state).isNotBlank();
        assertThat(java.net.URLDecoder.decode(redirectUri, java.nio.charset.StandardCharsets.UTF_8))
                .isEqualTo("http://localhost:8080/login/oauth2/code/" + provider);
        assertThat(oauthSession).isNotNull();
        mockMvc.perform(get("/login/oauth2/code/" + provider)
                        .session(oauthSession)
                        .param("code", "unused")
                        .param("state", "invalid-state"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?error"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"google", "github", "my-custom-auth"})
    void shouldRedirectOAuthProviderErrorsToFrontend(String provider) throws Exception {
        MvcResult authorizationResult = mockMvc.perform(get("/oauth2/authorization/" + provider))
                .andExpect(status().isFound())
                .andReturn();
        String state = UriComponentsBuilder.fromUriString(authorizationResult.getResponse().getHeader("Location"))
                .build()
                .getQueryParams()
                .getFirst("state");
        MockHttpSession oauthSession = (MockHttpSession) authorizationResult.getRequest().getSession(false);

        assertThat(state).isNotBlank();
        assertThat(oauthSession).isNotNull();
        mockMvc.perform(get("/login/oauth2/code/" + provider)
                        .session(oauthSession)
                        .param("error", "access_denied")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?error"));
    }

    @Test
    void shouldUseSameGenericFailureRedirectForUnknownAndIncorrectCredentials() throws Exception {
        CsrfSession csrf = fetchCsrf();
        mockMvc.perform(post("/login")
                        .session(csrf.session())
                        .param("username", "missing_user")
                        .param("password", "wrong-password")
                        .param(csrf.token().parameterName(), csrf.token().token()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?error"));

        User knownUser = createUser("known_user", "known@example.com", UserRole.ROLE_USER);

        mockMvc.perform(post("/login")
                        .session(csrf.session())
                        .param("username", "known_user")
                        .param("password", "wrong-password")
                        .param(csrf.token().parameterName(), csrf.token().token()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?error"));

        assertThat(userRepository.findById(knownUser.getId()).orElseThrow().getLastLogin())
                        .isNull();
    }

    @Test
    void registrationDoesNotAuthenticateTheNewAccount() throws Exception {
        CsrfSession csrf = fetchCsrf();

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration("new_user", "new@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_user"));

        mockMvc.perform(get("/api/auth/me").session(csrf.session()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("registration reports required fields and malformed email in English")
    void shouldReturnEnglishRegistrationValidationMessages() throws Exception {
        CsrfSession csrf = fetchCsrf();

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(RegisterRequest.builder()
                                .username("")
                                .password("")
                                .email("")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Please enter your username."),
                        org.hamcrest.Matchers.containsString("Please enter your password."),
                        org.hamcrest.Matchers.containsString("Please enter your email address.")
                )));

        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registration(
                                "invalid_email", "not-an-email"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Please enter a valid email address, like name@example.com."
                ));
    }

    @Test
    void shouldRejectRegistrationAndProtectedWritesWithoutValidCsrfTokens() throws Exception {
        String registrationJson = objectMapper.writeValueAsString(
                registration("missing_csrf", "missing-csrf@example.com")
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(registrationJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF Failed"));

        CsrfSession csrf = fetchCsrf();
        mockMvc.perform(post("/api/auth/register")
                        .session(csrf.session())
                        .header(csrf.token().headerName(), "invalid-token")
                        .contentType("application/json")
                        .content(registrationJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF Failed"));

        mockMvc.perform(patch("/api/users/me")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                UserUpdateRequest.builder().username("anonymous").build()
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CSRF Failed"));

        mockMvc.perform(patch("/api/users/me")
                        .session(csrf.session())
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                UserUpdateRequest.builder().username("anonymous").build()
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptCsrfHeaderOnAuthenticatedApiWrites() throws Exception {
        User user = createUser("csrf_write", "csrf-write@example.com", UserRole.ROLE_USER);
        CsrfSession csrf = fetchCsrf();
        UserUpdateRequest update = UserUpdateRequest.builder()
                .username("csrf_write_updated")
                .email("csrf-write-updated@example.com")
                .build();

        mockMvc.perform(patch("/api/users/me")
                        .session(csrf.session())
                        .with(authentication(authenticationFor(user)))
                        .header(csrf.token().headerName(), csrf.token().token())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("csrf_write_updated"));
    }

    @Test
    void shouldRequireCsrfForLogoutAndInvalidateSessionAfterValidLogout() throws Exception {
        createUser("logout_user", "logout@example.com", UserRole.ROLE_USER);
        CsrfSession anonymousCsrf = fetchCsrf();
        MvcResult loginResult = mockMvc.perform(post("/login")
                        .session(anonymousCsrf.session())
                        .param("username", "logout_user")
                        .param("password", "correct-password")
                        .param(anonymousCsrf.token().parameterName(), anonymousCsrf.token().token()))
                .andExpect(status().isFound())
                .andReturn();
        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/logout").session(authenticatedSession));
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").session(authenticatedSession))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?csrfError"));
        mockMvc.perform(post("/logout")
                        .session(authenticatedSession)
                        .param("_csrf", "invalid-token"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?csrfError"));
        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isOk());

        CsrfSession authenticatedCsrf = fetchCsrf(authenticatedSession);
        mockMvc.perform(post("/logout")
                        .session(authenticatedSession)
                        .param(authenticatedCsrf.token().parameterName(), authenticatedCsrf.token().token()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in?logout"));

        mockMvc.perform(get("/api/auth/me").session(authenticatedSession))
                .andExpect(status().isUnauthorized());
        assertThat(fetchCsrf().token().token()).isNotBlank();
    }

    @Test
    void shouldKeepSwaggerAuthenticatedAndInjectSessionCsrfForWrites() throws Exception {
        User user = createUser("swagger_user", "swagger@example.com", UserRole.ROLE_USER);

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost:5173/sign-in"));

        mockMvc.perform(get("/swagger-ui/index.html")
                        .with(authentication(authenticationFor(user))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/swagger-initializer.js")
                        .with(authentication(authenticationFor(user))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/auth/csrf")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("requestInterceptor")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("csrf.headerName")));

        mockMvc.perform(get("/v3/api-docs")
                        .with(authentication(authenticationFor(user))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldConfigureSessionCookieForHttpDevelopmentAndHttpsDeployment() {
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class))
                .isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.secure", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site"))
                .isEqualToIgnoringCase("lax");
    }

    private CsrfSession fetchCsrf() throws Exception {
        return fetchCsrf(null);
    }

    private CsrfSession fetchCsrf(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) {
            request.session(session);
        }

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn();
        MockHttpSession responseSession =
                (MockHttpSession) result.getRequest().getSession(false);
        return new CsrfSession(
                responseSession,
                objectMapper.readValue(
                        result.getResponse().getContentAsString(),
                        AuthController.CsrfTokenResponse.class
                )
        );
    }

    private RegisterRequest registration(String username, String email) {
        return RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("new-password")
                .build();
    }

    private UsernamePasswordAuthenticationToken authenticationFor(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private record CsrfSession(
            MockHttpSession session,
            AuthController.CsrfTokenResponse token
    ) {
    }

    private User createUser(String username, String email, UserRole role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("correct-password"))
                .roles(Set.of(role))
                .registeredAt(LocalDateTime.now())
                .build());
    }
}
