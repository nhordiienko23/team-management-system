package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
class CustomOAuth2UserServicesTest {
    @Test
    void customOAuth2ServiceWrapsResolvedUserAndProviderAttributes() {
        GitHubOAuth2AccountResolver resolver =
                mock(GitHubOAuth2AccountResolver.class);
        OAuth2UserRequest request = oauthRequest();
        User localUser = user();
        when(resolver.resolve(org.mockito.ArgumentMatchers.eq(request),
                org.mockito.ArgumentMatchers.any(OAuth2User.class)))
                .thenReturn(localUser);

        OAuth2User providerUser = mock(OAuth2User.class);
        when(providerUser.getAttributes())
                .thenReturn(Map.of("id", 123456L, "login", "octocat"));

        CustomOAuth2UserService wrappingService =
                new CustomOAuth2UserService(resolver) {
                    @Override
                    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
                        return new CustomUserDetails(
                                localUser,
                                providerUser.getAttributes()
                        );
                    }
                };

        OAuth2User result = wrappingService.loadUser(request);

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) result).getUser()).isSameAs(localUser);
    }

    @Test
    void customOidcServiceWrapsResolvedUserAndOidcClaims() {
        OidcAccountResolver resolver = mock(OidcAccountResolver.class);
        OidcUserRequest request = oidcRequest();
        User localUser = user();
        OidcUser providerUser = mock(OidcUser.class);
        when(providerUser.getAttributes()).thenReturn(Map.of("sub", "subject"));
        when(providerUser.getClaims()).thenReturn(Map.of("sub", "subject"));
        when(providerUser.getIdToken()).thenReturn(request.getIdToken());
        when(providerUser.getUserInfo()).thenReturn(null);
        when(resolver.resolve(org.mockito.ArgumentMatchers.any(OidcUser.class)))
                .thenReturn(localUser);

        CustomOidcUserService wrappingService =
                new CustomOidcUserService(resolver) {
                    @Override
                    public OidcUser loadUser(OidcUserRequest userRequest) {
                        return new CustomUserDetails(localUser, providerUser);
                    }
                };

        OidcUser result = wrappingService.loadUser(request);

        assertThat(result).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) result).getUser()).isSameAs(localUser);
    }

    private OAuth2UserRequest oauthRequest() {
        return new OAuth2UserRequest(githubRegistration(), accessToken());
    }

    private OidcUserRequest oidcRequest() {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Map.of("iss", "https://accounts.google.com",
                        "sub", "subject",
                        "email", "person@example.com")
        );
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("sub")
                .scope("openid", "profile", "email")
                .build();
        return new OidcUserRequest(registration, accessToken(), idToken);
    }

    private ClientRegistration githubRegistration() {
        return ClientRegistration.withRegistrationId("github")
                .clientId("test-client")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/github")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.test/user")
                .userNameAttributeName("id")
                .build();
    }

    private OAuth2AccessToken accessToken() {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        return new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Set.of("openid", "read:user")
        );
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("person")
                .email("person@example.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
    }
}
