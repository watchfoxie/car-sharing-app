package com.services.pricing_rules_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for Pricing Rules Service.
 * 
 * <p>Configures OAuth2 Resource Server with JWT validation against Keycloak:</p>
 * <ul>
 *   <li><strong>Authentication</strong>: Validates JWT tokens issued by Keycloak</li>
 *   <li><strong>Authorization</strong>: Extracts roles from {@code realm_access.roles} claim and maps to Spring Security authorities</li>
 *   <li><strong>CSRF</strong>: Disabled (stateless REST API)</li>
 *   <li><strong>Session</strong>: Stateless (no server-side sessions)</li>
 * </ul>
 * 
 * <p><strong>Public Endpoints (No Authentication Required):</strong></p>
 * <ul>
 *   <li>{@code /actuator/health} - Health checks for load balancers/monitoring</li>
 *   <li>{@code /actuator/info} - Application metadata</li>
 *   <li>{@code /swagger-ui/**} - Swagger UI (dev/staging only, disabled in prod via profile)</li>
 *   <li>{@code /v3/api-docs/**} - OpenAPI JSON spec</li>
 * </ul>
 * 
 * <p><strong>Protected Endpoints (Authentication Required):</strong></p>
 * <ul>
 *   <li>{@code /v1/pricing/**} - All pricing API endpoints (requires valid JWT Bearer token)</li>
 * </ul>
 * 
 * <p><strong>Method-Level Security:</strong></p>
 * <ul>
 *   <li>{@code @EnableMethodSecurity} allows {@code @PreAuthorize} annotations on service methods</li>
 *   <li>Example: {@code @PreAuthorize("hasRole('ADMIN')")} for admin-only operations</li>
 * </ul>
 * 
 * <p><strong>JWT Role Mapping:</strong></p>
 * <ul>
 *   <li>Keycloak realm roles are extracted from {@code realm_access.roles} claim</li>
 *   <li>Each role is prefixed with {@code ROLE_} (Spring Security convention)</li>
 *   <li>Example: Keycloak role {@code ADMIN} → Spring authority {@code ROLE_ADMIN}</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
 * @see org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @Secured, etc.
public class SecurityConfig {

    /**
     * Configures HTTP security for the application.
     * 
     * <p><strong>Security Flow:</strong></p>
     * <ol>
     *   <li>Client sends request with {@code Authorization: Bearer <jwt_token>} header</li>
     *   <li>Spring Security validates JWT signature and claims against Keycloak issuer-uri</li>
     *   <li>If valid, roles are extracted from {@code realm_access.roles} claim</li>
     *   <li>Request proceeds to controller with {@code Authentication} object populated</li>
     * </ol>
     * 
     * @param http HttpSecurity builder for configuring security rules
     * @return Configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled (stateless REST API with JWT)
            .csrf(csrf -> csrf.disable())
            
            // Authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (no authentication required)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll() // Dev/staging only (disabled via profile in prod)
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // Protected endpoints (authentication required)
                .requestMatchers("/v1/pricing/**").authenticated()
                
                // All other endpoints require authentication by default
                .anyRequest().authenticated()
            )
            
            // OAuth2 Resource Server with JWT validation
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()) // Custom role extraction
                )
            )
            
            // Stateless session management (no HttpSession)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * Configures custom JWT authentication converter to extract roles from Keycloak token.
     * 
     * <p><strong>Token Structure Example:</strong></p>
     * <pre>{@code
     * {
     *   "sub": "keycloak-user-id",
     *   "realm_access": {
     *     "roles": ["ADMIN", "OWNER"]
     *   },
     *   "email": "user@example.com",
     *   ...
     * }
     * }</pre>
     * 
     * <p><strong>Conversion Process:</strong></p>
     * <ol>
     *   <li>Extract {@code realm_access.roles} array from JWT</li>
     *   <li>Prefix each role with {@code ROLE_} (Spring Security convention)</li>
     *   <li>Convert to {@code GrantedAuthority} collection</li>
     *   <li>Create {@code Authentication} object with principal = JWT subject (user ID)</li>
     * </ol>
     * 
     * @return Configured JWT authentication converter
     */
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return jwtAuthenticationConverter;
    }

    /**
     * Custom converter to extract roles from {@code realm_access.roles} claim in Keycloak JWT.
     * 
     * <p><strong>Implementation Details:</strong></p>
     * <ul>
     *   <li>Navigates to {@code realm_access.roles} claim path</li>
     *   <li>Handles missing claims gracefully (returns empty list)</li>
     *   <li>Prefixes roles with {@code ROLE_} for Spring Security compatibility</li>
     * </ul>
     * 
     * @return Converter that extracts granted authorities from JWT
     */
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            // Extract realm_access.roles claim from Keycloak JWT
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return List.of(); // No roles found
            }

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");

            // Convert to GrantedAuthority with ROLE_ prefix
            return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        };
    }
}
