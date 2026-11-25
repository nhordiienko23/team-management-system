package com.nba.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final FrontendUrls frontendUrls;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CustomOAuth2UserService customOAuth2UserService,
                                                   CustomOidcUserService customOidcUserService) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
                        .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/sign-in",
                                "/sign-up",
                                "/api/auth/register",
                                "/api/auth/csrf",
                                "/login",
                                "/login/oauth2/**",
                                "/oauth2/authorization/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/sign-in")
                        .loginProcessingUrl("/login")
                        .failureUrl(frontendUrls.signIn() + "?error")
                        .defaultSuccessUrl(frontendUrls.home(), true)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl(frontendUrls.home(), true)
                        .failureUrl(frontendUrls.signIn() + "?error")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                                .oidcUserService(customOidcUserService))
                )
                .logout(logout -> logout
                        .logoutSuccessUrl(frontendUrls.signIn() + "?logout")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .build();
    }

    @Bean
    public JwtDecoderFactory<ClientRegistration> oidcIdTokenDecoderFactory(
            @Value("${app.oauth2.google-oidc-issuer}") String googleIssuer,
            @Value("${app.oauth2.custom-oidc-issuer}") String customIssuer) {
        Map<String, String> expectedIssuers = Map.of(
                "google", googleIssuer,
                "my-custom-auth", customIssuer
        );
        OidcIdTokenDecoderFactory decoderFactory = new OidcIdTokenDecoderFactory();
        decoderFactory.setJwtValidatorFactory(registration -> {
            String expectedIssuer = expectedIssuers.get(registration.getRegistrationId());
            if (expectedIssuer == null || expectedIssuer.isBlank()) {
                throw new IllegalStateException(
                        "No OIDC issuer is configured for registration " + registration.getRegistrationId());
            }
            OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(),
                    new OidcIdTokenValidator(registration),
                    new JwtIssuerValidator(expectedIssuer)
            );
            return validators;
        });
        return decoderFactory;
    }
}