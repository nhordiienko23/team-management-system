package com.nba.security;

import com.nba.user.User;
import com.nba.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginSuccessListener {
    private final UserRepository userRepository;

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            User user = userRepository.findById(customUserDetails.getUser().getId()).orElse(null);
            if (user != null) {
                user.setLastLogin(LocalDateTime.now());
            }
        }
    }
}
