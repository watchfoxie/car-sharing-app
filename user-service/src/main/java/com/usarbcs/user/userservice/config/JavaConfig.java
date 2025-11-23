package com.usarbcs.user.userservice.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({AdminSecurityProperties.class, JwtProperties.class})
public class JavaConfig {

    private static final String DEFAULT_ADMIN_USERNAME = "csadmin";
    private static final String DEFAULT_ADMIN_PASSWORD = "csadmin123";
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v1/auth/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder,
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

        return new InMemoryUserDetailsManager(adminUsers.values().toArray(UserDetails[]::new));
    }

    private UserDetails buildAdminUser(String username, String rawPassword, PasswordEncoder passwordEncoder) {
        return User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles("ADMIN")
                .build();
    }
}
