package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {
    @Test
    void exposesLocalUserContractForFormAuthentication() {
        User localUser = User.builder()
                .username("local-user")
                .password("encoded-password")
                .roles(Set.of(UserRole.ROLE_ADMIN, UserRole.ROLE_USER))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(localUser);

        assertThat(userDetails.getUser()).isSameAs(localUser);
        assertThat(userDetails.getUsername()).isEqualTo("local-user");
        assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
        assertThat(userDetails.getName()).isEqualTo("local-user");
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        assertThat(userDetails.getAttributes()).isNull();
        assertThat(userDetails.getClaims()).isNull();
        assertThat(userDetails.getIdToken()).isNull();
        assertThat(userDetails.getUserInfo()).isNull();
    }

    @Test
    void exposesOAuthAttributesAndNameForOAuthAuthentication() {
        User localUser = User.builder()
                .username("oauth-user")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
        Map<String, Object> attributes = Map.of("id", 123L, "login", "octocat");

        CustomUserDetails userDetails = new CustomUserDetails(localUser, attributes);

        assertThat(userDetails.getAttributes()).isSameAs(attributes);
        assertThat(userDetails.getClaims()).isSameAs(attributes);
        assertThat(userDetails.getName()).isEqualTo("oauth-user");
    }

    @Test
    void preservesOidcTokensAndUsesLocalAuthorities() {
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-01T01:00:00Z");
        OidcIdToken idToken = new OidcIdToken(
                "provider-token",
                issuedAt,
                expiresAt,
                Map.of("sub", "provider-subject", "email", "person@example.com")
        );
        OidcUserInfo userInfo = new OidcUserInfo(
                Map.of("sub", "provider-subject", "email", "person@example.com")
        );
        DefaultOidcUser providerUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("SCOPE_openid")),
                idToken,
                userInfo,
                "sub"
        );
        User localUser = User.builder()
                .username("local-user")
                .email("person@example.com")
                .roles(Set.of(UserRole.ROLE_ADMIN))
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(localUser, providerUser);

        assertThat(userDetails.getIdToken()).isSameAs(idToken);
        assertThat(userDetails.getUserInfo()).isSameAs(userInfo);
        assertThat(userDetails.getClaims())
                .containsEntry("sub", "provider-subject")
                .containsEntry("email", "person@example.com");
        assertThat(userDetails.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }
}
