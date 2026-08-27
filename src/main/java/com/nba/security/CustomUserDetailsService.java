package com.nba.security;

import com.nba.core.exception.notFound.UserNotFoundException;
import com.nba.user.User;
import com.nba.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class CustomUserDetailsService implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userService.getUserByUsername(username);
            return new CustomUserDetails(user);
        } catch (UserNotFoundException ex) {
            throw new UsernameNotFoundException("User not found");
        }

    }
}
