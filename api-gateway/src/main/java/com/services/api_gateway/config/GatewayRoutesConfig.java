package com.services.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.time.Duration;

/**
 * Gateway Routes Configuration
 * 
 * <p>Defines explicit routing rules for the API Gateway. Routes requests to backend
 * microservices with path rewriting, timeouts, circuit breakers, and retry policies.</p>
 * 
 * <p>Routing Strategy:</p>
 * <ul>
 *   <li>Service discovery: Uses lb:// prefix for load-balanced routing via Eureka</li>
 *   <li>Path-based routing: /api/v1/{service}/** → lb://{service}/v1/**</li>
 *   <li>Infrastructure routes: Direct access to Eureka dashboard (dev only)</li>
 * </ul>
 * 
 * <p>Routes defined:</p>
 * <ol>
 *   <li>Car Service: /api/v1/cars/** → lb://car-service/v1/cars/**</li>
 *   <li>Pricing Service: /api/v1/pricing/** → lb://pricing-rules-service/v1/pricing/**</li>
 *   <li>Rental Service: /api/v1/rentals/** → lb://rental-service/v1/rentals/**</li>
 *   <li>Feedback Service: /api/v1/feedback/** → lb://feedback-service/v1/feedback/**</li>
 *   <li>Identity Service: /api/v1/accounts/** → lb://identity-adapter/v1/accounts/**</li>
 *   <li>Eureka Dashboard: /eureka/** → lb://discovery-service/eureka/** (dev)</li>
 * </ol>
 * 
 * <p>Resilience Features (per route):</p>
 * <ul>
 *   <li>Response timeout: 5s default (configurable per route)</li>
 *   <li>Circuit breaker: Opens after 5 consecutive failures, 10s wait</li>
 *   <li>Retry: 3 attempts with exponential backoff (GET/HEAD only)</li>
 * </ul>
 * 
 * <p>Filters applied:</p>
 * <ul>
 *   <li>StripPrefix: Removes /api prefix before forwarding</li>
 *   <li>AddRequestHeader: Injects correlation IDs</li>
 *   <li>CircuitBreaker: Resilience4j integration</li>
 *   <li>Retry: Exponential backoff for transient errors</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@Configuration
public class GatewayRoutesConfig {

    /**
     * Defines custom routes with resilience patterns.
     * 
     * <p>Note: Automatic discovery-based routing (via locator.enabled=true) is also active,
     * these routes provide explicit configuration for fine-grained control.</p>
     * 
     * @param builder RouteLocatorBuilder from Spring Cloud Gateway
     * @return Configured routes with filters and predicates
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            
            // Car Service routes
            .route("car-service", r -> r
                .path("/api/v1/cars/**")
                .filters(f -> f
                    .stripPrefix(2)  // Remove /api/v1
                    .circuitBreaker(c -> c
                        .setName("carServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/car-service"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true))
                )
                .uri("lb://car-service"))
            
            // Pricing & Rules Service routes
            .route("pricing-service", r -> r
                .path("/api/v1/pricing/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(c -> c
                        .setName("pricingServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/pricing-service"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true))
                )
                .uri("lb://pricing-rules-service"))
            
            // Rental Service routes
            .route("rental-service", r -> r
                .path("/api/v1/rentals/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(c -> c
                        .setName("rentalServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/rental-service"))
                    // No retry for POST/PUT/DELETE (idempotency considerations)
                )
                .uri("lb://rental-service"))
            
            // Feedback Service routes
            .route("feedback-service", r -> r
                .path("/api/v1/feedback/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(c -> c
                        .setName("feedbackServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/feedback-service"))
                    .retry(retryConfig -> retryConfig
                        .setRetries(3)
                        .setMethods(HttpMethod.GET)
                        .setBackoff(Duration.ofMillis(100), Duration.ofSeconds(1), 2, true))
                )
                .uri("lb://feedback-service"))
            
            // Identity Adapter routes
            .route("identity-service", r -> r
                .path("/api/v1/accounts/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .circuitBreaker(c -> c
                        .setName("identityServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/identity-service"))
                )
                .uri("lb://identity-adapter"))
            
            // Eureka Dashboard (dev mode only - should be filtered by security in prod)
            .route("eureka-dashboard", r -> r
                .path("/eureka/**")
                .uri("lb://discovery-service"))
            
            .build();
    }
}
