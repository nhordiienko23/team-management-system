package com.nba.auth;

import com.nba.security.CustomUserDetails;
import com.nba.user.UserShortDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserShortDto> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.register(registerRequest));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfTokenResponse> getCsrfToken(
            @RequestAttribute("_csrf") CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new CsrfTokenResponse(
                        csrfToken.getToken(),
                        csrfToken.getParameterName(),
                        csrfToken.getHeaderName()
                ));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(new AuthenticatedUserResponse(
                currentUser.getUser().getId(),
                currentUser.getUsername(),
                currentUser.getUser().getEmail(),
                currentUser.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        ));
    }

    public record AuthenticatedUserResponse(
            Long id,
            String username,
            String email,
            List<String> authorities
    ) {
    }

    public record CsrfTokenResponse(
            String token,
            String parameterName,
            String headerName
    ) {
    }
}
