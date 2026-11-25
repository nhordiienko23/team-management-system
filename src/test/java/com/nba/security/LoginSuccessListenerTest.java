package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginSuccessListenerTest {
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LoginSuccessListener listener = new LoginSuccessListener(userRepository);

    @Test
    void updatesLastLoginAfterSuccessfulLocalAuthentication() {
        assertLastLoginRecorded(new CustomUserDetails(newUser()));
    }

    @Test
    void updatesLastLoginAfterSuccessfulGoogleOidcAuthentication() {
        assertLastLoginRecorded(oidcPrincipal());
    }

    @Test
    void updatesLastLoginAfterSuccessfulGitHubOauthAuthentication() {
        assertLastLoginRecorded(new CustomUserDetails(newUser(), Map.of("id", 123L)));
    }

    @Test
    void updatesLastLoginAfterSuccessfulCustomOidcAuthentication() {
        assertLastLoginRecorded(oidcPrincipal());
    }

    @Test
    void authenticationFailureDoesNotUpdateLastLogin() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(UserRepository.class, () -> userRepository);
            context.registerBean(LoginSuccessListener.class);
            context.refresh();
            context.publishEvent(new AuthenticationFailureBadCredentialsEvent(
                    new UsernamePasswordAuthenticationToken("test-user", "wrong-password"),
                    new BadCredentialsException("Invalid credentials")
            ));
        }

        verifyNoInteractions(userRepository);
    }

    private void assertLastLoginRecorded(CustomUserDetails principal) {
        User persistedUser = newUser();
        when(userRepository.findById(persistedUser.getId())).thenReturn(Optional.of(persistedUser));

        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(
                new UsernamePasswordAuthenticationToken(principal, null)
        ));

        assertThat(persistedUser.getLastLogin()).isNotNull();
        verify(userRepository).findById(persistedUser.getId());
    }

    private static CustomUserDetails oidcPrincipal() {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        OidcIdToken idToken = new OidcIdToken(
                "test-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Map.of("sub", "provider-subject", "email", "person@example.com")
        );
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(),
                idToken,
                "sub"
        );
        return new CustomUserDetails(newUser(), oidcUser);
    }

    private static User newUser() {
        return User.builder()
                .id(42L)
                .username("test-user")
                .email("person@example.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .lastLogin((LocalDateTime) null)
                .build();
    }
}
