package com.nba.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendUrlsTest {

    @Test
    void usesConfiguredOriginWithoutDuplicatingTrailingSlashes() {
        FrontendUrls urls = new FrontendUrls("https://frontend.example/");

        assertThat(urls.home()).isEqualTo("https://frontend.example/");
        assertThat(urls.signIn()).isEqualTo("https://frontend.example/sign-in");
    }
}
