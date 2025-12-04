package com.usarbcs.gateway.config;


import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

@Component
public class RouterValidator {

    private static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/",
            "/__gateway/info",
            "/v1/auth",
            "/v1/auth/register",
            "/v1/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator/**",
            "/favicon.ico",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean isSecured(ServerHttpRequest request) {
        if (request.getMethod() == HttpMethod.OPTIONS) {
            return false;
        }
        final String requestPath = request.getURI().getPath();
        return OPEN_API_ENDPOINTS.stream()
                .noneMatch(uri -> pathMatcher.match(uri, requestPath));
    }
}
