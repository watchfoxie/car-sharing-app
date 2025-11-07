package com.services.rental_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for Rental Service.
 * <p>
 * Configures OAuth2 Resource Server with JWT validation against Keycloak.
 * Extracts roles from {@code realm_access.roles} claim and maps them with {@code ROLE_} prefix.
 * </p>
 * <p>
 * <strong>Public endpoints:</strong>
 * <ul>
 *   <li>/actuator/health - Health check (monitoring)</li>
 *   <li>/actuator/info - Service info (monitoring)</li>
 *   <li>/swagger-ui/** - Swagger UI (dev/staging only)</li>
 *   <li>/v3/api-docs/** - OpenAPI JSON (dev/staging only)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Protected endpoints:</strong>
 * <ul>
 *   <li>/v1/rentals/** - All rental endpoints require authentication</li>
 *   <li>Fine-grained authorization via @PreAuthorize in controllers</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configure security filter chain with OAuth2 Resource Server.
     *
     * @param http HttpSecurity builder
     * @return SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )

                // Stateless session management (no HttpSession)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        return http.build();
    }

    /**
     * JWT authentication converter with custom role extraction.
     * <p>
     * Extracts roles from {@code realm_access.roles} claim in Keycloak JWT.
     * Adds {@code ROLE_} prefix to each role for Spring Security compatibility.
     * </p>
     *
     * @return JWT authentication converter
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    /**
     * Custom JWT granted authorities converter.
     * <p>
     * Extracts roles from {@code realm_access.roles} claim and maps them to GrantedAuthority.
     * Example JWT claim structure:
     * <pre>
     * {
     *   "realm_access": {
     *     "roles": ["RENTER", "OWNER"]
     *   }
     * }
     * </pre>
     * Mapped authorities: {@code ROLE_RENTER}, {@code ROLE_OWNER}
     * </p>
     *
     * @return converter that extracts roles from JWT
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        };
    }
}
