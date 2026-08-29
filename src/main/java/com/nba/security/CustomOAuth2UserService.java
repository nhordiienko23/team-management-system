package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserService userService;

    @Override
    public @Nullable OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            email = oAuth2User.getAttribute("login") + "@github.com";
        }
        String finalEmail = email;

        String username = oAuth2User.getAttribute("name");
        if (username == null) {
            username = oAuth2User.getAttribute("login");
        }
        String finalUsername = username;

        User user = userService.findByEmail(finalEmail)
                .orElseGet(() -> userService.createOAuth2User(finalUsername, finalEmail));
        user.setLastLogin(LocalDateTime.now());
        userService.saveToDatabase(user);


        return new CustomUserDetails(user, oAuth2User.getAttributes());

    }

}
