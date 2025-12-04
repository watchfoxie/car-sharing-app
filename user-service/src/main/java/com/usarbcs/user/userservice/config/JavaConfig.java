package com.usarbcs.user.userservice.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties({AdminSecurityProperties.class, JwtProperties.class})
public class JavaConfig {

    private static final String DEFAULT_ADMIN_USERNAME = "csadmin";
    private static final String DEFAULT_ADMIN_PASSWORD = "csadmin123";
        private static final String[] PUBLIC_ENDPOINTS = {
            "/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/webjars/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info"
    };

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyExchange().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder,
                                                                 AdminSecurityProperties adminSecurityProperties) {
        Map<String, UserDetails> adminUsers = new LinkedHashMap<>();
        adminUsers.put(DEFAULT_ADMIN_USERNAME,
                buildAdminUser(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD, passwordEncoder));

        String configuredUsername = adminSecurityProperties.username();
        String configuredPassword = adminSecurityProperties.password();
        if (StringUtils.hasText(configuredUsername)
                && StringUtils.hasText(configuredPassword)
                && !DEFAULT_ADMIN_USERNAME.equals(configuredUsername)) {
            adminUsers.put(configuredUsername,
                    buildAdminUser(configuredUsername, configuredPassword, passwordEncoder));
        }

                return new MapReactiveUserDetailsService(adminUsers.values().toArray(UserDetails[]::new));
    }

    private UserDetails buildAdminUser(String username, String rawPassword, PasswordEncoder passwordEncoder) {
        return User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles("ADMIN")
                .build();
    }
}
