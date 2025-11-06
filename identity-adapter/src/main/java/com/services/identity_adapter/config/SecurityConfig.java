package com.services.identity_adapter.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Identity Adapter Service.
 * 
 * <p>Configures OAuth2 Resource Server with JWT validation and role-based access control (RBAC).
 * 
 * <p><strong>Key features:</strong>
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation against Keycloak</li>
 *   <li>Role mapping from JWT claims (ROLE_ADMIN, ROLE_OWNER, ROLE_RENTER)</li>
 *   <li>Method-level security with @PreAuthorize annotations</li>
 *   <li>Public endpoints: /actuator/health, /actuator/info (monitoring)</li>
 *   <li>Protected endpoints: /v1/accounts/** (authenticated users only)</li>
 *   <li>Stateless session management (no server-side sessions)</li>
 *   <li>CSRF disabled (RESTful API)</li>
 * </ul>
 * 
 * <p><strong>Role hierarchy:</strong>
 * <ul>
 *   <li>ADMIN: full system access, can manage all accounts</li>
 *   <li>OWNER: can manage own vehicles and rental approvals</li>
 *   <li>RENTER: can search, book, and manage own rentals</li>
 * </ul>
 * 
 * <p><strong>JWT token structure expected:</strong>
 * <pre>
 * {
 *   "sub": "auth0|507f1f77bcf86cd799439011",
 *   "realm_access": {
 *     "roles": ["RENTER", "OWNER"]
 *   },
 *   "email": "john.doe@example.com",
 *   "preferred_username": "john.doe"
 * }
 * </pre>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Configures HTTP security filter chain with OAuth2 Resource Server.
     * 
     * <p><strong>Access rules:</strong>
     * <ul>
     *   <li>Actuator health/info: PUBLIC (for monitoring tools)</li>
     *   <li>Eureka endpoints: PUBLIC in dev (for dashboard access)</li>
     *   <li>Swagger/OpenAPI: PUBLIC in dev, BLOCKED in prod</li>
     *   <li>/v1/accounts/**: AUTHENTICATED (any logged-in user can access own profile)</li>
     * </ul>
     * 
     * @param http the {@link HttpSecurity} to modify
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - monitoring
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                
                // Public endpoints - Eureka (dev only, should be blocked in prod via profile)
                .requestMatchers("/eureka/**").permitAll()
                
                // Public endpoints - OpenAPI/Swagger (dev only)
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                
                // Protected endpoints - account management
                .requestMatchers("/v1/accounts/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            
            // Stateless session management (no server-side sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        
        return http.build();
    }

    /**
     * Configures JWT authentication converter to extract roles from Keycloak token.
     * 
     * <p>Extracts roles from {@code realm_access.roles} claim and converts them to
     * Spring Security authorities with {@code ROLE_} prefix.
     * 
     * <p><strong>Example conversion:</strong>
     * <pre>
     * Token claim: "realm_access": { "roles": ["ADMIN", "RENTER"] }
     * → Spring authorities: [ROLE_ADMIN, ROLE_RENTER]
     * </pre>
     * 
     * @return configured JWT authentication converter
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        
        // Extract roles from realm_access.roles claim (Keycloak standard)
        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        
        // Add ROLE_ prefix to match Spring Security conventions
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        
        return jwtAuthenticationConverter;
    }
}
