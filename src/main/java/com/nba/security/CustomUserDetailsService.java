package com.nba.security;

import com.nba.user.UserService;
import com.nba.core.exception.invalidData.InvalidUserDataException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        try {
            return new CustomUserDetails(userService.findByUsernameOrEmail(login));
        } catch (InvalidUserDataException exception) {
            logger.warn("Authentication failed because the submitted account was not found");
            throw new UsernameNotFoundException("Invalid username or password", exception);
        }
    }
}
