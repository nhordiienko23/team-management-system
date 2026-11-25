package com.nba.security;

import com.nba.user.User;
import jakarta.annotation.Nullable;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User, OidcUser {
    private final User user;

    private final Map<String, Object> attributes;
    private final Map<String, Object> claims;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public CustomUserDetails(User user) {
        this(user, null, null, null, null);
    }

    public CustomUserDetails(User user, Map<String, Object> attributes) {
        this(user, attributes, attributes, null, null);
    }

    public CustomUserDetails(User user, OidcUser oidcUser) {
        this(
                user,
                oidcUser.getAttributes(),
                oidcUser.getClaims(),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo()
        );
    }

    private CustomUserDetails(
            User user,
            Map<String, Object> attributes,
            Map<String, Object> claims,
            OidcIdToken idToken,
            OidcUserInfo userInfo) {
        this.user = user;
        this.attributes = attributes;
        this.claims = claims;
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return getUsername();
    }

    @Override
    public Map<String, Object> getClaims() {
        return claims;
    }


    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }


    @Override
    public OidcUserInfo getUserInfo() {
        return userInfo;
    }

    @Override
    public OidcIdToken getIdToken() {
        return idToken;
    }
}
