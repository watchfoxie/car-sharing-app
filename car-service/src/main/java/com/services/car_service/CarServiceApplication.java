package com.services.car_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main application class for Car Service.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD operations for vehicle fleet management</li>
 *   <li>Advanced search and filtering (brand, category, price, availability)</li>
 *   <li>Sorting (A-Z/Z-A by brand, price ascending/descending)</li>
 *   <li>Owner-based access control (owners manage their own cars)</li>
 *   <li>Redis caching for public car listings</li>
 *   <li>Integration with Feedback Service for rating aggregation</li>
 * </ul>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>Port: 8082</li>
 *   <li>Database: PostgreSQL schema 'car'</li>
 *   <li>Eureka client enabled for service discovery</li>
 *   <li>OAuth2 Resource Server for JWT authentication</li>
 *   <li>Redis cache for performance optimization</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableCaching
public class CarServiceApplication {

    /**
     * Main entry point for the Car Service application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(CarServiceApplication.class, args);
    }
}
