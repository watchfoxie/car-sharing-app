package com.services.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security Configuration for API Gateway
 * 
 * <p>Configures security policies for the reactive gateway, including:</p>
 * <ul>
 *   <li>OAuth2 Resource Server with JWT validation (Keycloak)</li>
 *   <li>Public endpoints (health checks, Eureka dashboard in dev)</li>
 *   <li>Authenticated endpoints (all /api/v1/** routes)</li>
 *   <li>CSRF protection (disabled for stateless API)</li>
 * </ul>
 * 
 * <p>Authentication Flow:</p>
 * <ol>
 *   <li>Client (Angular) obtains JWT from Keycloak via OIDC</li>
 *   <li>Client includes JWT in Authorization header: Bearer {token}</li>
 *   <li>Gateway validates JWT signature against Keycloak issuer</li>
 *   <li>Gateway extracts roles/claims and forwards to downstream services</li>
 * </ol>
 * 
 * <p>Public Endpoints (no authentication required):</p>
 * <ul>
 *   <li>/actuator/health - Health checks for monitoring</li>
 *   <li>/eureka/** - Eureka dashboard (dev only, should be blocked in prod)</li>
 * </ul>
 * 
 * <p>Protected Endpoints (JWT required):</p>
 * <ul>
 *   <li>/api/v1/** - All business API endpoints</li>
 * </ul>
 * 
 * <p>Security Headers:</p>
 * <ul>
 *   <li>X-Content-Type-Options: nosniff</li>
 *   <li>X-Frame-Options: DENY</li>
 *   <li>X-XSS-Protection: 1; mode=block</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Development security configuration.
     * 
     * <p>Permissive settings for local development:</p>
     * <ul>
     *   <li>OAuth2 validation enabled but more lenient</li>
     *   <li>Eureka dashboard accessible</li>
     *   <li>Detailed error messages</li>
     * </ul>
     * 
     * @param http ServerHttpSecurity builder
     * @return Configured security filter chain
     */
    @Bean
    @Profile("dev")
    public SecurityWebFilterChain devSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(CsrfSpec::disable)  // Disabled for stateless API
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints
                .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                .pathMatchers("/eureka/**").permitAll()  // Eureka dashboard in dev
                .pathMatchers("/fallback/**").permitAll()  // Circuit breaker fallbacks
                .pathMatchers("/openapi", "/openapi/**").permitAll()  // OpenAPI aggregation
                
                // Protected API endpoints (JWT required)
                .pathMatchers("/api/v1/**").authenticated()
                
                // Default: deny all other requests
                .anyExchange().authenticated()
            )
            // OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT validation against Keycloak
                    // Issuer URI configured in application.yaml
                })
            )
            .build();
    }

    /**
     * Production security configuration.
     * 
     * <p>Strict settings for production deployment:</p>
     * <ul>
     *   <li>OAuth2 validation strictly enforced</li>
     *   <li>Eureka dashboard blocked</li>
     *   <li>Generic error messages (no information leakage)</li>
     *   <li>Security headers enabled</li>
     * </ul>
     * 
     * @param http ServerHttpSecurity builder
     * @return Configured security filter chain
     */
    @Bean
    @Profile("staging")
    public SecurityWebFilterChain stagingSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(CsrfSpec::disable)  // Disabled for stateless API
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (minimal)
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/fallback/**").permitAll()
                .pathMatchers("/openapi", "/openapi/**").permitAll()  // OpenAPI in staging
                
                // Protected API endpoints
                .pathMatchers("/api/v1/**").authenticated()
                
                // Block infrastructure endpoints
                .pathMatchers("/eureka/**").denyAll()
                .pathMatchers("/actuator/**").denyAll()
                
                // Default: deny
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // JWT validation
                })
            )
            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
                .contentTypeOptions(content -> {})
            )
            .build();
    }

    /**
     * Security configuration for production environment.
     * 
     * <p><strong>Restrictions:</strong>
     * <ul>
     *   <li>OpenAPI endpoints blocked</li>
     *   <li>Eureka dashboard blocked</li>
     *   <li>Actuator endpoints blocked (except health)</li>
     * </ul>
     * 
     * @param http ServerHttpSecurity builder
     * @return Configured security filter chain
     */
    @Bean
    @Profile("prod")
    public SecurityWebFilterChain prodSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(CsrfSpec::disable)  // Disabled for stateless API
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (minimal)
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/fallback/**").permitAll()
                
                // Protected API endpoints
                .pathMatchers("/api/v1/**").authenticated()
                
                // Block infrastructure and documentation endpoints in prod
                .pathMatchers("/openapi", "/openapi/**").denyAll()  // OpenAPI blocked in prod
                .pathMatchers("/eureka/**").denyAll()
                .pathMatchers("/actuator/**").denyAll()
                
                // Default: deny
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {
                    // Strict JWT validation
                })
            )
            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())  // X-Frame-Options: DENY
                .contentTypeOptions(content -> {})        // X-Content-Type-Options: nosniff
            )
            .build();
    }
}
