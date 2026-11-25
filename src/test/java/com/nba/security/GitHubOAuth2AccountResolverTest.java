package com.nba.security;

import com.nba.user.ExternalIdentity;
import com.nba.user.ExternalIdentityRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import com.nba.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubOAuth2AccountResolverTest {
    @Mock
    private ExternalIdentityRepository externalIdentityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private GitHubEmailClient gitHubEmailClient;

    private GitHubOAuth2AccountResolver resolver;
    private ExternalIdentityService externalIdentityService;

    @BeforeEach
    void setUp() {
        externalIdentityService = new ExternalIdentityService(
                externalIdentityRepository,
                userRepository,
                userService
        );
        resolver = new GitHubOAuth2AccountResolver(externalIdentityService, gitHubEmailClient);
    }

    @Test
    void linkedAccountUsesStableNumericGitHubIdWithoutEmailLookup() {
        User linkedUser = user(7L, "linked@example.com");
        when(externalIdentityRepository.findByProviderAndSubject("github", "123456"))
                .thenReturn(java.util.Optional.of(new ExternalIdentity("github", "123456", linkedUser)));

        User resolved = resolver.resolve(request(), githubUser(123456L, "linked@example.com"));

        assertThat(resolved).isSameAs(linkedUser);
        verify(gitHubEmailClient, never()).findVerifiedPrimaryEmail(any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void verifiedPrimaryEmailCanLinkExistingLocalAccount() {
        User localUser = user(7L, "person@example.com");
        when(gitHubEmailClient.findVerifiedPrimaryEmail(any()))
                .thenReturn(java.util.Optional.of("person@example.com"));
        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));

        OAuth2UserRequest userRequest = request();
        User resolved = resolver.resolve(userRequest, githubUser(123456L, null));

        assertThat(resolved).isSameAs(localUser);
        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
        verify(userService, never()).createOAuth2User(any(), any());
    }

    @Test
    void unverifiedOrMissingEmailCannotMatchProfileEmailOrSyntheticAddress() {
        when(gitHubEmailClient.findVerifiedPrimaryEmail(any()))
                .thenReturn(java.util.Optional.empty());

        OAuth2UserRequest userRequest = request();
        assertThatThrownBy(() -> resolver.resolve(
                userRequest,
                githubUser(123456L, "victim@example.com")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("External authentication failed");

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(userService, never()).createOAuth2User(any(), any());
        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void missingPublicEmailDoesNotPreventIdentityLookupButCannotCreateAnAccountWithoutVerification() {
        when(gitHubEmailClient.findVerifiedPrimaryEmail(any()))
                .thenReturn(java.util.Optional.empty());
        OAuth2UserRequest userRequest = request();

        assertThatThrownBy(() -> resolver.resolve(userRequest, githubUser(123456L, null)))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
        verify(userService, never()).createOAuth2User(any(), any());
    }

    @Test
    void verifiedEmailCollisionIsCheckedOnlyAgainstTrustedGitHubEmailResponse() {
        User localUser = user(7L, "person@example.com");
        when(gitHubEmailClient.findVerifiedPrimaryEmail(any()))
                .thenReturn(java.util.Optional.of("person@example.com"));
        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(localUser));
        when(externalIdentityRepository.existsByUser_IdAndProvider(7L, "github"))
                .thenReturn(true);

        OAuth2UserRequest userRequest = request();
        assertThatThrownBy(() -> resolver.resolve(
                userRequest,
                githubUser(123456L, "person@example.com")))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void rejectsMissingOrInvalidStableGitHubId() {
        assertThatThrownBy(() -> resolver.resolve(
                request(),
                githubUser("account-login", "person@example.com")))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(gitHubEmailClient, never()).findVerifiedPrimaryEmail(any());
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .username("local-" + id)
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
    }

    private DefaultOAuth2User githubUser(Object id, String profileEmail) {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("id", id);
        attributes.put("login", "sample-login");
        attributes.put("name", "Sample User");
        if (profileEmail != null) {
            attributes.put("email", profileEmail);
        }
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("SCOPE_read:user")),
                attributes,
                "id"
        );
    }

    private OAuth2UserRequest request() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/github")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Set.of("read:user", "user:email")
        );
        return new OAuth2UserRequest(registration, accessToken);
    }
}
