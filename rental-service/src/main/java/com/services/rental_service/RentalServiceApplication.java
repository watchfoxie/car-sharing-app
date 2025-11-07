package com.services.rental_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the Rental Service.
 * <p>
 * This service manages the complete rental lifecycle for the Car Sharing platform,
 * implementing a Finite State Machine (FSM) for rental workflow orchestration:
 * PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED (+ CANCELLED).
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Rental creation with conflict-free booking (EXCLUDE constraint on rental_period)</li>
 *   <li>FSM state transitions: pickup, return, return approval, cancellation</li>
 *   <li>Idempotency support via (renter_id, idempotency_key) unique constraint</li>
 *   <li>Cost calculation integration with pricing-rules-service</li>
 *   <li>Concurrency handling with retry logic for EXCLUDE violations</li>
 *   <li>Audit trail for all rental lifecycle events</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Database schema:</strong> {@code rental} (PostgreSQL with btree_gist extension)
 * </p>
 * <p>
 * <strong>Port:</strong> 8084
 * </p>
 * <p>
 * <strong>Discovery:</strong> Registers with Eureka Server as {@code rental-service}
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableScheduling
public class RentalServiceApplication {

    /**
     * Main entry point for the Rental Service application.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        SpringApplication.run(RentalServiceApplication.class, args);
    }
}
