package com.nba.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityExceptionHandlersTest {
    private final FrontendUrls frontendUrls = new FrontendUrls("https://frontend.example");

    @Test
    void apiAuthenticationFailureReturnsJsonUnauthorized() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAuthenticationEntryPoint(frontendUrls).commence(
                request,
                response,
                new InsufficientAuthenticationException("missing")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"status\":401")
                .contains("\"error\":\"Unauthorized\"");
    }

    @Test
    void browserAuthenticationFailureRedirectsToSignIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/players");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAuthenticationEntryPoint(frontendUrls).commence(
                request,
                response,
                new InsufficientAuthenticationException("missing")
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).isEqualTo("https://frontend.example/sign-in");
    }

    @Test
    void csrfLoginFailureRedirectsWithCsrfError() throws Exception {
        assertCsrfRedirect("/login");
        assertCsrfRedirect("/logout");
    }

    @Test
    void csrfApiFailureReturnsJsonForbidden() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/players");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAccessDeniedHandler(frontendUrls).handle(
                request,
                response,
                new CsrfException("missing token")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"error\":\"CSRF Failed\"");
    }

    @Test
    void regularAccessDeniedFailureReturnsJsonForbidden() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAccessDeniedHandler(frontendUrls).handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("forbidden")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                .contains("\"error\":\"Forbidden\"");
    }

    private void assertCsrfRedirect(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new CustomAccessDeniedHandler(frontendUrls).handle(
                request,
                response,
                new CsrfException("missing token")
        );

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://frontend.example/sign-in?csrfError");
    }
}
