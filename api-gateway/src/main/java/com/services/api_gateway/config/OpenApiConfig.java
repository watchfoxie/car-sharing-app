package com.services.api_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for OpenAPI aggregation components.
 * 
 * <p>Provides beans required by {@link com.services.api_gateway.controller.OpenApiAggregationController}:
 * <ul>
 *   <li>WebClient.Builder for reactive HTTP calls to services</li>
 *   <li>ObjectMapper for JSON processing</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Configuration
public class OpenApiConfig {

    /**
     * Load-balanced WebClient builder for service-to-service communication.
     * 
     * <p>Uses Eureka service discovery to resolve service names to actual instances.
     * 
     * @return WebClient builder with load balancing
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * ObjectMapper for JSON serialization/deserialization.
     * 
     * @return configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
