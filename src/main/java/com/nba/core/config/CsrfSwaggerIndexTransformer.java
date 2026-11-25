package com.nba.core.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CsrfSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

    private static final String INITIALIZER = "window.ui = SwaggerUIBundle({";
    private static final String REQUEST_INTERCEPTOR = """
            requestInterceptor: async (request) => {
              const method = (request.method || "GET").toUpperCase();
              const url = new URL(request.url, window.location.origin);
              const safeMethods = ["GET", "HEAD", "OPTIONS", "TRACE"];
              if (url.origin === window.location.origin && !safeMethods.includes(method)) {
                const response = await fetch("/api/auth/csrf", {
                  credentials: "same-origin",
                  cache: "no-store"
                });
                if (!response.ok) {
                  throw new Error("Unable to obtain a CSRF token for this request.");
                }
                const csrf = await response.json();
                if (typeof csrf.token !== "string" || typeof csrf.headerName !== "string") {
                  throw new Error("The server returned an invalid CSRF token response.");
                }
                request.headers = request.headers || {};
                request.headers[csrf.headerName] = csrf.token;
              }
              return request;
            },
            """;

    public CsrfSwaggerIndexTransformer(
            SwaggerUiConfigProperties swaggerUiConfig,
            SwaggerUiOAuthProperties swaggerUiOAuthProperties,
            SwaggerUiConfigParameters swaggerUiConfigParameters,
            SwaggerWelcomeCommon swaggerWelcomeCommon,
            ObjectMapperProvider objectMapperProvider) {
        super(
                swaggerUiConfig,
                swaggerUiOAuthProperties,
                swaggerUiConfigParameters,
                swaggerWelcomeCommon,
                objectMapperProvider
        );
    }

    @Override
    public Resource transform(
            HttpServletRequest request,
            Resource resource,
            ResourceTransformerChain transformerChain) throws IOException {
        Resource transformed = super.transform(request, resource, transformerChain);
        if (!resource.getURL().toString().contains("swagger-initializer.js")) {
            return transformed;
        }

        String initializer;
        try (InputStream input = transformed.getInputStream()) {
            initializer = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!initializer.contains(INITIALIZER)) {
            throw new IllegalStateException("Springdoc Swagger initializer format is not supported.");
        }

        String customizedInitializer = initializer.replace(
                INITIALIZER,
                INITIALIZER + "\n" + REQUEST_INTERCEPTOR
        );
        return new TransformedResource(
                transformed,
                customizedInitializer.getBytes(StandardCharsets.UTF_8)
        );
    }
}
