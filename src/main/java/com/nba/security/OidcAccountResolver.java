
package com.nba.security;

import com.nba.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OidcAccountResolver {

    private static final Logger logger =
            LoggerFactory.getLogger(OidcAccountResolver.class);

    private final ExternalIdentityService externalIdentityService;
    private final Set<String> trustedIssuers;
    private final Set<String> emailVerificationExemptIssuers;

    public OidcAccountResolver(
            ExternalIdentityService externalIdentityService,
            @Value("${app.oauth2.trusted-oidc-issuers:https://accounts.google.com}")
            String trustedIssuerProperty,
            @Value("${app.oauth2.email-verification-exempt-issuers:}")
            String emailVerificationExemptIssuerProperty) {

        this.externalIdentityService = externalIdentityService;
        this.trustedIssuers = parseIssuers(trustedIssuerProperty);
        this.emailVerificationExemptIssuers =
                parseIssuers(emailVerificationExemptIssuerProperty);
    }

    public User resolve(OidcUser oidcUser) throws OAuth2AuthenticationException {
        URL issuer = oidcUser.getIdToken().getIssuer();
        String subject = oidcUser.getIdToken().getSubject();

        if (issuer == null || !StringUtils.hasText(subject)) {
            throw OAuthAuthenticationFailures.genericFailure();
        }

        String issuerValue = issuer.toString();

        if (!trustedIssuers.contains(issuerValue)) {
            logger.warn(
                    "OIDC authentication rejected because issuer is not trusted: {}",
                    issuerValue
            );
            throw OAuthAuthenticationFailures.genericFailure();
        }

        String email = firstNonBlank(
                oidcUser.getEmail(),
                oidcUser.getIdToken().getClaimAsString("email")
        );

        boolean emailVerificationExempt =
                emailVerificationExemptIssuers.contains(issuerValue);

        boolean emailVerified = emailVerificationExempt
                || Boolean.TRUE.equals(oidcUser.getAttribute("email_verified"))
                || Boolean.TRUE.equals(
                oidcUser.getIdToken().getClaim("email_verified")
        );

        String displayName = firstNonBlank(
                oidcUser.getAttribute("name"),
                oidcUser.getAttribute("preferred_username"),
                oidcUser.getAttribute("given_name"),
                oidcUser.getFullName()
        );

        try {
            return externalIdentityService.resolve(
                    issuerValue,
                    subject,
                    () -> emailVerified && StringUtils.hasText(email)
                            ? Optional.of(email.trim())
                            : Optional.empty(),
                    displayName
            );
        } catch (DataIntegrityViolationException exception) {
            logger.warn(
                    "OIDC authentication rejected because an external identity mapping conflicted"
            );
            throw OAuthAuthenticationFailures.genericFailure();
        }
    }

    private Set<String> parseIssuers(String property) {
        return Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String firstNonBlank(String... candidates) {
        return Arrays.stream(candidates)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
