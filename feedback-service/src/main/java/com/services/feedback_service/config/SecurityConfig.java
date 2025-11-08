package com.services.feedback_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for Feedback Service.
 * 
 * <p>Implements OAuth2 Resource Server with JWT validation against Keycloak.
 * 
 * <p>Access control:
 * <ul>
 *   <li><b>Public:</b> GET /v1/feedback/cars/** (aggregations, summaries, distribution)</li>
 *   <li><b>Authenticated:</b> POST /v1/feedback, GET /v1/feedback/my</li>
 *   <li><b>Admin:</b> GET /v1/feedback/reports/** (reports and analytics)</li>
 * </ul>
 * 
 * <p>JWT validation:
 * <ul>
 *   <li>Issuer: Keycloak realm {@code car-sharing}</li>
 *   <li>Roles: Extracted from {@code realm_access.roles} claim</li>
 *   <li>Prefix: {@code ROLE_} added automatically</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Configures HTTP security with OAuth2 Resource Server.
     * 
     * @param http the HTTP security builder
     * @return configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (aggregations, summaries)
                .requestMatchers("/v1/feedback/cars/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                
                // Swagger UI (dev/staging only)
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Authenticated endpoints (feedback submission, user profile)
                .requestMatchers("/v1/feedback/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(csrf -> csrf.disable()) // Stateless API, CSRF not needed
            .cors(Customizer.withDefaults()); // CORS handled by API Gateway

        return http.build();
    }

    /**
     * Configures JWT authentication converter to extract roles from Keycloak token.
     * 
     * <p>Extracts roles from {@code realm_access.roles} claim and prefixes with {@code ROLE_}.
     * 
     * @return JWT authentication converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract standard scopes
            JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

            // Extract Keycloak realm roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                Collection<GrantedAuthority> realmAuthorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
                authorities.addAll(realmAuthorities);
            }

            return authorities;
        });
        return converter;
    }
}
