package com.nba.core.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi coachApi() {
        return GroupedOpenApi.builder()
                .group("coach API")
                .pathsToMatch("/api/coaches/**")
                .build();
    }

    @Bean
    public GroupedOpenApi teamApi() {
        return GroupedOpenApi.builder()
                .group("team API")
                .pathsToMatch("/api/teams/**")
                .build();
    }

    @Bean
    public GroupedOpenApi playerApi() {
        return GroupedOpenApi.builder()
                .group("player API")
                .pathsToMatch("/api/players/**")
                .build();
    }
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("user API")
                .pathsToMatch("/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminUserApi() {
        return GroupedOpenApi.builder()
                .group("user API for admins")
                .pathsToMatch("/api/admin/users/**")
                .build();
    }
}
