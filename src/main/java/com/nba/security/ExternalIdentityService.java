
package com.nba.security;

import com.nba.user.ExternalIdentity;
import com.nba.user.ExternalIdentityRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ExternalIdentityService {

    private static final Logger logger =
            LoggerFactory.getLogger(ExternalIdentityService.class);

    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public User resolve(
            String provider,
            String subject,
            Supplier<Optional<String>> emailSupplier,
            String displayName) {

        if (!StringUtils.hasText(provider) || !StringUtils.hasText(subject)) {
            return reject(provider, "missing stable provider identity");
        }

        Optional<ExternalIdentity> linkedIdentity =
                externalIdentityRepository.findByProviderAndSubject(provider, subject);

        if (linkedIdentity.isPresent()) {
            return linkedIdentity.get().getUser();
        }

        Optional<String> email = emailSupplier.get()
                .filter(StringUtils::hasText)
                .map(String::trim);

        if (email.isEmpty()) {
            return reject(provider, "email is missing");
        }

        List<User> matchingUsers =
                userRepository.findWithRolesByEmailIgnoreCase(email.get());

        if (matchingUsers.size() > 1) {
            return reject(provider, "email matches multiple local accounts");
        }

        User user;

        if (matchingUsers.size() == 1) {
            user = matchingUsers.get(0);

            if (externalIdentityRepository.existsByUser_IdAndProvider(
                    user.getId(), provider)) {
                return reject(provider,
                        "local account already has a different identity for this provider");
            }
        } else {
            user = userService.createOAuth2User(
                    resolveDisplayName(displayName, email.get()),
                    email.get()
            );
        }

        externalIdentityRepository.save(
                new ExternalIdentity(provider, subject, user)
        );

        return user;
    }

    private String resolveDisplayName(String displayName, String email) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }

        int atIndex = email.indexOf('@');

        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return "oauth_user";
    }

    private User reject(String provider, String reason) {
        logger.warn(
                "External authentication rejected for provider {}: {}",
                provider,
                reason
        );

        throw OAuthAuthenticationFailures.genericFailure();
    }
}
