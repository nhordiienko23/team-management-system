package com.nba.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class GitHubEmailClient {
    private static final Logger logger = LoggerFactory.getLogger(GitHubEmailClient.class);
    private static final ParameterizedTypeReference<List<GitHubEmail>> EMAIL_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String emailEndpoint;

    public GitHubEmailClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.oauth2.github-email-uri:https://api.github.com/user/emails}") String emailEndpoint) {
        this.restClient = restClientBuilder.build();
        this.emailEndpoint = emailEndpoint;
    }

    public Optional<String> findVerifiedPrimaryEmail(OAuth2UserRequest userRequest) {
        OAuth2AccessToken accessToken = userRequest.getAccessToken();
        try {
            List<GitHubEmail> emails = restClient.get()
                    .uri(emailEndpoint)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken.getTokenValue());
                        headers.set("User-Agent", "team-management-system");
                        headers.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .accept(MediaType.parseMediaType("application/vnd.github+json"))
                    .retrieve()
                    .body(EMAIL_LIST_TYPE);
            if (emails == null) {
                throw failure("GitHub returned an invalid email response");
            }

            List<String> verifiedPrimaryEmails = emails.stream()
                    .filter(email -> Boolean.TRUE.equals(email.primary()) && Boolean.TRUE.equals(email.verified()))
                    .map(GitHubEmail::email)
                    .filter(email -> email != null && !email.isBlank())
                    .toList();
            if (verifiedPrimaryEmails.size() > 1) {
                throw failure("GitHub returned ambiguous verified primary emails");
            }
            return verifiedPrimaryEmails.stream().findFirst();
        } catch (RestClientException exception) {
            logger.warn("GitHub verified-email lookup failed: {}", exception.getClass().getSimpleName());
            throw OAuthAuthenticationFailures.genericFailure();
        }
    }

    private OAuth2AuthenticationException failure(String reason) {
        logger.warn("GitHub verified-email lookup failed: {}", reason);
        return OAuthAuthenticationFailures.genericFailure();
    }

    private record GitHubEmail(String email, Boolean primary, Boolean verified) {
    }
}
