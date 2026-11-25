package com.nba.security;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

final class OAuthAuthenticationFailures {
    private OAuthAuthenticationFailures() {
    }

    static OAuth2AuthenticationException genericFailure() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("authentication_failed"),
                "External authentication failed"
        );
    }
}
