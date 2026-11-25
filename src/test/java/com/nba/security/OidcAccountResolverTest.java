package com.nba.security;

import com.nba.user.ExternalIdentity;
import com.nba.user.ExternalIdentityRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import com.nba.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OidcAccountResolverTest {

    private static final String GOOGLE_ISSUER =
            "https://accounts.google.com";

    private static final String CUSTOM_ISSUER =
            "http://localhost:9000";

    @Mock
    private ExternalIdentityRepository externalIdentityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    private OidcAccountResolver resolver;

    @BeforeEach
    void setUp() {
        ExternalIdentityService externalIdentityService =
                new ExternalIdentityService(
                        externalIdentityRepository,
                        userRepository,
                        userService
                );

        resolver = new OidcAccountResolver(
                externalIdentityService,
                GOOGLE_ISSUER + "," + CUSTOM_ISSUER,
                CUSTOM_ISSUER
        );
    }

    @Test
    @DisplayName("Should link verified Google email to existing local account")
    void linksVerifiedGoogleEmailToExistingLocalAccount() {
        User localUser = user(11L, "person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        User resolved = resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "person@example.com",
                        true
                )
        );

        assertThat(resolved).isSameAs(localUser);

        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
        verify(userService, never()).createOAuth2User(any(), any());
    }

    @Test
    @DisplayName("Should create a new account for verified Google email")
    void createsNewAccountForVerifiedGoogleEmail() {
        User newUser = user(11L, "new-person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("new-person@example.com"))
                .thenReturn(List.of());

        when(userService.createOAuth2User(
                "Test User",
                "new-person@example.com"
        )).thenReturn(newUser);

        User resolved = resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "new-person@example.com",
                        true
                )
        );

        assertThat(resolved).isSameAs(newUser);

        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    @DisplayName("Should reject unverified Google email")
    void rejectsUnverifiedGoogleEmail() {
        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "person@example.com",
                        false
                )
        )).isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("External authentication failed");

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject Google email without email_verified claim")
    void rejectsMissingGoogleEmailVerifiedClaim() {
        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "person@example.com",
                        null
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
    }

    @Test
    @DisplayName("Should allow custom issuer without email_verified claim")
    void allowsCustomIssuerWithoutEmailVerifiedClaim() {
        User localUser = user(11L, "person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        User resolved = resolver.resolve(
                oidcUser(
                        CUSTOM_ISSUER,
                        "custom-subject",
                        "person@example.com",
                        null
                )
        );

        assertThat(resolved).isSameAs(localUser);

        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    @DisplayName("Should allow custom issuer with false email_verified claim when exempt")
    void allowsCustomIssuerWithFalseEmailVerifiedClaimWhenExempt() {
        User localUser = user(11L, "person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        User resolved = resolver.resolve(
                oidcUser(
                        CUSTOM_ISSUER,
                        "custom-subject",
                        "person@example.com",
                        false
                )
        );

        assertThat(resolved).isSameAs(localUser);

        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    @DisplayName("Should reject custom issuer when email is missing")
    void rejectsCustomIssuerWithoutEmail() {
        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        CUSTOM_ISSUER,
                        "custom-subject",
                        null,
                        null
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(userService, never()).createOAuth2User(any(), any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject untrusted issuer")
    void rejectsUntrustedIssuer() {
        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        "https://untrusted.example",
                        "subject",
                        "person@example.com",
                        true
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should use email from ID token when UserInfo email is missing")
    void usesEmailFromIdTokenWhenUserInfoEmailIsMissing() {
        User localUser = user(11L, "person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        User resolved = resolver.resolve(
                oidcUserWithSeparateClaims(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "person@example.com",
                        true,
                        null
                )
        );

        assertThat(resolved).isSameAs(localUser);

        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    @DisplayName("Should reject missing email even when email_verified is true")
    void rejectsMissingEmailEvenWhenVerified() {
        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        null,
                        true
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(userService, never()).createOAuth2User(any(), any());
    }

    @Test
    @DisplayName("Should resolve existing linked identity without checking email")
    void resolvesExistingLinkedIdentityWithoutCheckingEmail() {
        User linkedUser = user(11L, "first@example.com");

        ExternalIdentity linkedIdentity = new ExternalIdentity(
                GOOGLE_ISSUER,
                "google-subject",
                linkedUser
        );

        when(externalIdentityRepository.findByProviderAndSubject(
                GOOGLE_ISSUER,
                "google-subject"
        )).thenReturn(Optional.of(linkedIdentity));

        User resolved = resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "google-subject",
                        "different-local@example.com",
                        false
                )
        );

        assertThat(resolved).isSameAs(linkedUser);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject linking when local account already has another identity from issuer")
    void rejectsLinkWhenLocalAccountAlreadyHasAnotherIdentityFromIssuer() {
        User localUser = user(11L, "person@example.com");

        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        when(externalIdentityRepository.existsByUser_IdAndProvider(
                11L,
                GOOGLE_ISSUER
        )).thenReturn(true);

        assertThatThrownBy(() -> resolver.resolve(
                oidcUser(
                        GOOGLE_ISSUER,
                        "second-subject",
                        "person@example.com",
                        true
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);

        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void rejectsMissingIssuerOrSubject() {
        OidcIdToken missingIssuerToken = new OidcIdToken(
                "test-id-token",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                Map.of("sub", "subject")
        );
        DefaultOidcUser missingIssuer = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("SCOPE_openid")),
                missingIssuerToken,
                "sub"
        );
        assertThatThrownBy(() -> resolver.resolve(missingIssuer))
                .isInstanceOf(OAuth2AuthenticationException.class);

        OidcIdToken missingSubjectToken = new OidcIdToken(
                "test-id-token",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                Map.of("iss", GOOGLE_ISSUER)
        );
        DefaultOidcUser missingSubject = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("SCOPE_openid")),
                missingSubjectToken,
                "iss"
        );
        assertThatThrownBy(() -> resolver.resolve(missingSubject))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void mapsIdentityStorageConflictToGenericAuthenticationFailure() {
        ExternalIdentityService conflictingService =
                org.mockito.Mockito.mock(ExternalIdentityService.class);
        when(conflictingService.resolve(
                org.mockito.ArgumentMatchers.eq(GOOGLE_ISSUER),
                org.mockito.ArgumentMatchers.eq("google-subject"),
                any(),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DataIntegrityViolationException("conflict"));
        OidcAccountResolver conflictingResolver = new OidcAccountResolver(
                conflictingService,
                GOOGLE_ISSUER,
                ""
        );

        assertThatThrownBy(() -> conflictingResolver.resolve(
                oidcUser(GOOGLE_ISSUER, "google-subject",
                        "person@example.com", true)))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("External authentication failed");
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .username("local-" + id)
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
    }

    private DefaultOidcUser oidcUser(
            String issuer,
            String subject,
            String email,
            Boolean emailVerified
    ) {
        return oidcUserWithSeparateClaims(
                issuer,
                subject,
                email,
                emailVerified,
                email
        );
    }

    private DefaultOidcUser oidcUserWithSeparateClaims(
            String issuer,
            String subject,
            String idTokenEmail,
            Boolean emailVerified,
            String userInfoEmail
    ) {
        Map<String, Object> idTokenClaims = new HashMap<>();
        idTokenClaims.put("iss", issuer);
        idTokenClaims.put("sub", subject);
        idTokenClaims.put("name", "Test User");

        if (idTokenEmail != null) {
            idTokenClaims.put("email", idTokenEmail);
        }

        if (emailVerified != null) {
            idTokenClaims.put("email_verified", emailVerified);
        }

        Map<String, Object> userInfoClaims = new HashMap<>();
        userInfoClaims.put("sub", subject);
        userInfoClaims.put("name", "Test User");

        if (userInfoEmail != null) {
            userInfoClaims.put("email", userInfoEmail);
        }

        if (emailVerified != null) {
            userInfoClaims.put("email_verified", emailVerified);
        }

        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");

        OidcIdToken token = new OidcIdToken(
                "test-id-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                idTokenClaims
        );

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("SCOPE_openid")),
                token,
                new OidcUserInfo(userInfoClaims),
                "sub"
        );
    }
}
