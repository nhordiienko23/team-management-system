package com.nba.security;

import com.nba.user.ExternalIdentity;
import com.nba.user.ExternalIdentityRepository;
import com.nba.user.User;
import com.nba.user.UserRepository;
import com.nba.user.UserRole;
import com.nba.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalIdentityServiceTest {
    @Mock
    private ExternalIdentityRepository externalIdentityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    private ExternalIdentityService service;

    @BeforeEach
    void setUp() {
        service = new ExternalIdentityService(
                externalIdentityRepository,
                userRepository,
                userService
        );
    }

    @Test
    void rejectsMissingProviderOrSubject() {
        assertThatThrownBy(() -> service.resolve(" ", "subject",
                () -> Optional.of("person@example.com"), "Person"))
                .isInstanceOf(OAuth2AuthenticationException.class);
        assertThatThrownBy(() -> service.resolve("github", " ",
                () -> Optional.of("person@example.com"), "Person"))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(externalIdentityRepository, never()).findByProviderAndSubject(any(), any());
    }

    @Test
    void returnsAlreadyLinkedUserBeforeEmailLookup() {
        User linked = user(1L, "linked@example.com");
        when(externalIdentityRepository.findByProviderAndSubject("github", "123"))
                .thenReturn(Optional.of(new ExternalIdentity("github", "123", linked)));

        User result = service.resolve("github", "123", () -> {
            throw new AssertionError("email lookup must not run");
        }, "Person");

        assertThat(result).isSameAs(linked);
        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
    }

    @Test
    void trimsAndNormalizesEmailForMatching() {
        User local = user(2L, "person@example.com");
        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(local));

        User result = service.resolve("github", "123",
                () -> Optional.of("  person@example.com  "), "Person");

        assertThat(result).isSameAs(local);
        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    void rejectsEmptyEmailAfterSupplierResultIsTrimmed() {
        assertThatThrownBy(() -> service.resolve("github", "123",
                () -> Optional.of("   "), "Person"))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).findWithRolesByEmailIgnoreCase(any());
    }

    @Test
    void rejectsEmailMatchingMultipleLocalAccounts() {
        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of(user(1L, "person@example.com"), user(2L, "person@example.com")));

        assertThatThrownBy(() -> service.resolve("github", "123",
                () -> Optional.of("person@example.com"), "Person"))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(externalIdentityRepository, never()).save(any());
    }

    @Test
    void createsAccountWithDisplayNameFallbackFromEmail() {
        User created = user(3L, "person@example.com");
        when(userRepository.findWithRolesByEmailIgnoreCase("person@example.com"))
                .thenReturn(List.of());
        when(userService.createOAuth2User("person", "person@example.com"))
                .thenReturn(created);

        User result = service.resolve("github", "123",
                () -> Optional.of("person@example.com"), " ");

        assertThat(result).isSameAs(created);
        verify(externalIdentityRepository).save(any(ExternalIdentity.class));
    }

    @Test
    void createsAccountWithTrimmedDisplayNameAndGenericFallback() {
        User created = user(4L, "invalid-address");
        when(userRepository.findWithRolesByEmailIgnoreCase("invalid-address"))
                .thenReturn(List.of());
        when(userService.createOAuth2User("OAuth Person", "invalid-address"))
                .thenReturn(created);

        assertThat(service.resolve("github", "123",
                () -> Optional.of("invalid-address"), "  OAuth Person  "))
                .isSameAs(created);

        User fallback = user(5L, "invalid-address");
        when(userService.createOAuth2User("oauth_user", "invalid-address"))
                .thenReturn(fallback);
        assertThat(service.resolve("github", "124",
                () -> Optional.of("invalid-address"), null))
                .isSameAs(fallback);
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .username("user-" + id)
                .email(email)
                .roles(Set.of(UserRole.ROLE_USER))
                .build();
    }
}
