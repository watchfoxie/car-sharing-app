package com.services.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS Configuration for API Gateway
 * 
 * <p>Configures Cross-Origin Resource Sharing (CORS) policy for the Car Sharing platform.
 * Controls which origins (Angular frontend) can access the backend APIs.</p>
 * 
 * <p>Security Considerations:</p>
 * <ul>
 *   <li>Dev: Permissive (* allowed) for local development</li>
 *   <li>Staging/Prod: Strict whitelist of trusted domains</li>
 *   <li>Credentials: Allowed for cookie/JWT-based authentication</li>
 *   <li>Pre-flight: OPTIONS requests automatically handled</li>
 * </ul>
 * 
 * <p>Configuration properties (from config-repo/api-gateway.yaml):</p>
 * <pre>
 * cors:
 *   allowed-origins: "http://localhost:4200"  # Angular dev server
 *   allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
 *   allowed-headers: "*"
 *   max-age: 3600
 * </pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    /**
     * Creates CORS filter for reactive gateway.
     * 
     * <p>Applies to all routes (/**) with configured policy.</p>
     * 
     * @return CorsWebFilter configured with environment-specific settings
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Parse allowed origins (comma-separated)
        if ("*".equals(allowedOrigins)) {
            corsConfig.addAllowedOriginPattern("*");
        } else {
            Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .forEach(corsConfig::addAllowedOrigin);
        }
        
        // Parse allowed methods
        Arrays.stream(allowedMethods.split(","))
            .map(String::trim)
            .forEach(corsConfig::addAllowedMethod);
        
        // Parse allowed headers
        if ("*".equals(allowedHeaders)) {
            corsConfig.addAllowedHeader("*");
        } else {
            Arrays.stream(allowedHeaders.split(","))
                .map(String::trim)
                .forEach(corsConfig::addAllowedHeader);
        }
        
        // Allow credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Preflight cache duration
        corsConfig.setMaxAge(maxAge);
        
        // Expose headers that client can read
        corsConfig.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Total-Count",
            "X-Request-Id",
            "X-Trace-Id"
        ));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
