package com.services.pricing_rules_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Pricing Rules Service.
 * 
 * <p>This service is responsible for managing pricing rules and calculating costs
 * for vehicle rentals based on time intervals (MINUTE, HOUR, DAY), vehicle categories,
 * and operational policies (min/max duration, cancellation windows, late penalties).</p>
 * 
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>CRUD operations for pricing rules with temporal validity constraints</li>
 *   <li>Cost calculation engine supporting minute/hour/day-based pricing</li>
 *   <li>Validation of rental duration against configured rules</li>
 *   <li>Multi-tier caching strategy (Redis + Caffeine) for performance</li>
 *   <li>Scheduled jobs for rule expiration and cache warming</li>
 *   <li>OAuth2 Resource Server with JWT validation</li>
 *   <li>Observability via Actuator, Prometheus metrics, and Zipkin tracing</li>
 * </ul>
 * 
 * <p><strong>Annotations:</strong></p>
 * <ul>
 *   <li>{@code @SpringBootApplication} - Standard Spring Boot application configuration</li>
 *   <li>{@code @EnableDiscoveryClient} - Registers with Eureka for service discovery</li>
 *   <li>{@code @EnableCaching} - Activates Spring Cache abstraction for Redis and Caffeine</li>
 *   <li>{@code @EnableScheduling} - Enables Quartz-based scheduled jobs (rule expiration, cache warm-up)</li>
 * </ul>
 * 
 * <p><strong>Architecture:</strong></p>
 * <ul>
 *   <li>Database: PostgreSQL schema {@code pricing} with temporal constraints (EXCLUDE on effective_period)</li>
 *   <li>Cache: Two-tier strategy - Redis (10m TTL) for distributed sharing, Caffeine (5m TTL) for local lookups</li>
 *   <li>Security: OAuth2 Resource Server validating JWTs from Keycloak</li>
 *   <li>Messaging: Kafka integration for publishing pricing change events (Outbox pattern)</li>
 * </ul>
 * 
 * <p><strong>Port Configuration:</strong></p>
 * <ul>
 *   <li>Default port: 8083 (configurable via {@code server.port})</li>
 *   <li>Context path: {@code /} (direct access to {@code /v1/pricing/**})</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.cloud.client.discovery.EnableDiscoveryClient
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableScheduling
public class PricingRulesServiceApplication {

    /**
     * Application entry point.
     * 
     * <p>Starts the Spring Boot application context and initializes all configured beans.
     * The application will:</p>
     * <ol>
     *   <li>Connect to PostgreSQL and execute Flyway migrations</li>
     *   <li>Register with Eureka Discovery Service</li>
     *   <li>Initialize Redis and Caffeine cache managers</li>
     *   <li>Schedule Quartz jobs (rule expiration at 02:00 daily, cache warm-up)</li>
     *   <li>Expose REST API endpoints at {@code /v1/pricing/**}</li>
     *   <li>Expose Actuator endpoints at {@code /actuator/**}</li>
     * </ol>
     * 
     * @param args Command-line arguments (typically profile selection: {@code --spring.profiles.active=dev})
     */
    public static void main(String[] args) {
        SpringApplication.run(PricingRulesServiceApplication.class, args);
    }
}
