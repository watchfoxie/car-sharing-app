package com.usarbcs.authen.service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(AdminUserProperties.class)
public class SecurityConfig {

    private static final String DEFAULT_ADMIN_USERNAME = "csadmin";
    private static final String DEFAULT_ADMIN_PASSWORD = "csadmin123";
    private static final String[] ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/actuator",
            "/actuator/health",
            "/actuator/health/**",
            "/v1/auth/**"
    };

    @Bean
    public SecurityWebFilterChain authSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(ENDPOINTS)
                        .permitAll()
                        .anyExchange()
                        .authenticated())
                .build();
    }

            @Bean
            public MapReactiveUserDetailsService userDetailsService(AdminUserProperties adminUserProperties,
                                                                   PasswordEncoder passwordEncoder) {
                String username = adminUserProperties.username() == null ? DEFAULT_ADMIN_USERNAME : adminUserProperties.username();
                String rawPassword = adminUserProperties.password() == null ? DEFAULT_ADMIN_PASSWORD : adminUserProperties.password();
                String[] roles = adminUserProperties.roles() == null
                        ? new String[] {"ADMIN"}
                        : adminUserProperties.roles().toArray(String[]::new);

                return new MapReactiveUserDetailsService(
                        User.withUsername(username)
                                .password(passwordEncoder.encode(rawPassword))
                                .roles(roles)
                                .build()
                );
        }
}
