package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final UserService userService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String username = oidcUser.getAttribute("name");
        if (username == null) {
            username = oidcUser.getAttribute("given_name");
        }
        if (username == null) {
            username = oidcUser.getFullName();
        }
        if (username == null) {
            username = oidcUser.getName();
        }
        String finalUsername = username;

        User user = userService.findByEmail(email)
                .orElseGet(() -> userService.createOAuth2User(finalUsername, email));

        user.setLastLogin(LocalDateTime.now());
        userService.saveToDatabase(user);


        return new CustomUserDetails(user, oidcUser.getAttributes());
    }
}
