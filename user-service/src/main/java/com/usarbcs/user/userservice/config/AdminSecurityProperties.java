package com.usarbcs.user.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.admin")
public record AdminSecurityProperties(String username, String password) {
}
