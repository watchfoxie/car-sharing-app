package com.services.car_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for Car Service.
 * 
 * <p>Configures:
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation against Keycloak</li>
 *   <li>Role extraction from {@code realm_access.roles} claim</li>
 *   <li>Public endpoints: /actuator/health, /actuator/info, Swagger UI (dev)</li>
 *   <li>Protected endpoints: /v1/cars/** (authenticated users)</li>
 *   <li>Method-level security with @PreAuthorize</li>
 *   <li>Stateless session management</li>
 *   <li>CSRF disabled (API-only service)</li>
 * </ul>
 * 
 * <p>Security model:
 * <ul>
 *   <li>All authenticated users can read public cars (shareable=true)</li>
 *   <li>Owners can CRUD their own cars (validated in service layer)</li>
 *   <li>Admins have full access (if needed in future phases)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Configures HTTP security for the Car Service.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Stateless API
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                // Public endpoints
                auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
                
                // Swagger UI (dev/staging only)
                if ("dev".equals(activeProfile) || "staging".equals(activeProfile)) {
                    auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    ).permitAll();
                }
                
                // All other endpoints require authentication
                auth.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    /**
     * Converter for extracting roles from Keycloak JWT token.
     * 
     * <p>Extracts roles from {@code realm_access.roles} claim and maps them
     * to Spring Security GrantedAuthority with "ROLE_" prefix.
     *
     * @return the JWT authentication converter
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
