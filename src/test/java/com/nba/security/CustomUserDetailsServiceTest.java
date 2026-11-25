package com.nba.security;

import com.nba.core.exception.invalidData.InvalidUserDataException;
import com.nba.user.User;
import com.nba.user.UserRole;
import com.nba.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {
    private final UserService userService = mock(UserService.class);
    private final CustomUserDetailsService service =
            new CustomUserDetailsService(userService);

    @Test
    void loadsCustomUserDetailsByUsernameOrEmail() {
        User user = User.builder()
                .username("person")
                .email("person@example.com")
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
        when(userService.findByUsernameOrEmail("person@example.com")).thenReturn(user);

        assertThat(service.loadUserByUsername("person@example.com"))
                .isInstanceOfSatisfying(CustomUserDetails.class,
                        details -> assertThat(details.getUser()).isSameAs(user));
        verify(userService).findByUsernameOrEmail("person@example.com");
    }

    @Test
    void translatesMissingUserToUsernameNotFound() {
        when(userService.findByUsernameOrEmail("missing"))
                .thenThrow(new InvalidUserDataException("missing"));

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid username or password")
                .hasCauseInstanceOf(InvalidUserDataException.class);
    }
}
