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

    @Test
    void supportsOriginsWithoutTrailingSlashAndMultipleTrailingSlashes() {
        FrontendUrls noSlash = new FrontendUrls("https://frontend.example");
        FrontendUrls multipleSlashes = new FrontendUrls("https://frontend.example///");

        assertThat(noSlash.home()).isEqualTo("https://frontend.example/");
        assertThat(noSlash.signIn()).isEqualTo("https://frontend.example/sign-in");
        assertThat(multipleSlashes.home()).isEqualTo("https://frontend.example/");
    }

    @Test
    void emptyOriginProducesRootRelativeUrls() {
        FrontendUrls urls = new FrontendUrls("");

        assertThat(urls.home()).isEqualTo("/");
        assertThat(urls.signIn()).isEqualTo("/sign-in");
    }
}
