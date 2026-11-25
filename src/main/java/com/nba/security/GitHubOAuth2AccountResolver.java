package com.nba.security;

import com.nba.user.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GitHubOAuth2AccountResolver {
    private static final Logger logger = LoggerFactory.getLogger(GitHubOAuth2AccountResolver.class);
    private static final String PROVIDER = "github";

    private final ExternalIdentityService externalIdentityService;
    private final GitHubEmailClient gitHubEmailClient;

    public User resolve(OAuth2UserRequest userRequest, OAuth2User githubUser) {
        if (!PROVIDER.equals(userRequest.getClientRegistration().getRegistrationId())) {
            throw OAuthAuthenticationFailures.genericFailure();
        }

        Object rawId = githubUser.getAttribute("id");
        if (!(rawId instanceof Number numericId) || numericId.longValue() <= 0) {
            logger.warn("GitHub authentication rejected because the stable numeric user ID is missing");
            throw OAuthAuthenticationFailures.genericFailure();
        }
        String subject = numericId.toString();
        if (!subject.matches("[0-9]+")) {
            logger.warn("GitHub authentication rejected because the stable numeric user ID is invalid");
            throw OAuthAuthenticationFailures.genericFailure();
        }
        String displayName = githubUser.getAttribute("name");
        if (displayName == null || displayName.isBlank()) {
            displayName = githubUser.getAttribute("login");
        }
        String finalDisplayName = displayName;

        try {
            return externalIdentityService.resolve(
                    PROVIDER,
                    subject,
                    () -> gitHubEmailClient.findVerifiedPrimaryEmail(userRequest),
                    finalDisplayName
            );
        } catch (DataIntegrityViolationException exception) {
            logger.warn("GitHub authentication rejected because an external identity mapping conflicted");
            throw OAuthAuthenticationFailures.genericFailure();
        }
    }
}
