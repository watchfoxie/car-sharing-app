package com.services.feedback_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Feedback Service.
 * 
 * <p>This service manages customer feedback (ratings and comments) for vehicles
 * in the car-sharing platform. It provides functionality for:
 * <ul>
 *   <li>Creating feedback for completed rentals</li>
 *   <li>Retrieving feedback by car with aggregations (average rating, count)</li>
 *   <li>Anti-abuse policies (duplicate prevention, rate limiting)</li>
 *   <li>Reports and analytics (top cars, rating distribution)</li>
 * </ul>
 * 
 * <p>Key features:
 * <ul>
 *   <li><b>Eureka Discovery:</b> Registers with discovery-service for service mesh</li>
 *   <li><b>JPA Auditing:</b> Automatic tracking of created/modified timestamps and actors</li>
 *   <li><b>OAuth2 Resource Server:</b> JWT validation with Keycloak</li>
 *   <li><b>Anti-abuse:</b> Rate limiting and duplicate prevention</li>
 * </ul>
 * 
 * <p>Database: PostgreSQL schema {@code feedback}
 * <p>Port: 8085 (default)
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class FeedbackServiceApplication {

    /**
     * Main entry point for the Feedback Service application.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(FeedbackServiceApplication.class, args);
    }
}
