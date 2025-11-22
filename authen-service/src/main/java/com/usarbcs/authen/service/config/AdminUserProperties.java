package com.usarbcs.authen.service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security.admin")
public record AdminUserProperties(
        String username,
        String password,
        List<String> roles
) {
}
