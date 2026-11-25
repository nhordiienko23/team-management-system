package com.nba.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrontendUrls {

    private final String origin;

    public FrontendUrls(@Value("${app.frontend-origin:http://localhost:5173}") String origin) {
        this.origin = origin.replaceAll("/+$", "");
    }

    public String home() {
        return origin + "/";
    }

    public String signIn() {
        return origin + "/sign-in";
    }
}
