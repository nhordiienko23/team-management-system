package com.nba.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.http.HttpMethod.GET;

class GitHubEmailClientTest {
    private static final String EMAIL_ENDPOINT = "https://api.github.test/user/emails";

    @Test
    void returnsOnlyVerifiedPrimaryGitHubEmail() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EMAIL_ENDPOINT))
                .andExpect(method(GET))
                .andExpect(header("Authorization", "Bearer test-access-token"))
                .andExpect(header("User-Agent", "team-management-system"))
                .andRespond(withSuccess("""
                        [
                          {"email":"unverified@example.com","primary":true,"verified":false},
                          {"email":"secondary@example.com","primary":false,"verified":true},
                          {"email":"verified@example.com","primary":true,"verified":true}
                        ]
                        """, MediaType.APPLICATION_JSON));
        GitHubEmailClient client = new GitHubEmailClient(builder, EMAIL_ENDPOINT);

        assertThat(client.findVerifiedPrimaryEmail(userRequest()))
                .contains("verified@example.com");
        server.verify();
    }

    @Test
    void doesNotTrustUnverifiedPrimaryOrVerifiedSecondaryAddresses() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EMAIL_ENDPOINT))
                .andRespond(withSuccess("""
                        [
                          {"email":"unverified@example.com","primary":true,"verified":false},
                          {"email":"secondary@example.com","primary":false,"verified":true}
                        ]
                        """, MediaType.APPLICATION_JSON));
        GitHubEmailClient client = new GitHubEmailClient(builder, EMAIL_ENDPOINT);

        assertThat(client.findVerifiedPrimaryEmail(userRequest())).isEmpty();
        server.verify();
    }

    @Test
    void failsAuthenticationIfGitHubEmailVerificationEndpointFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(EMAIL_ENDPOINT)).andRespond(withServerError());
        GitHubEmailClient client = new GitHubEmailClient(builder, EMAIL_ENDPOINT);

        assertThatThrownBy(() -> client.findVerifiedPrimaryEmail(userRequest()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessage("External authentication failed");
        server.verify();
    }

    private OAuth2UserRequest userRequest() {
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
