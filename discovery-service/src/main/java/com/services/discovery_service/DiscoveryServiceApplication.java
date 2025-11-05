package com.services.discovery_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Discovery Service Application - Eureka Server
 * 
 * <p>Central service registry for the Car Sharing microservices architecture.
 * Provides service discovery, health checking, and load balancing capabilities.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Service registration and heartbeat monitoring</li>
 *   <li>Dynamic service discovery for client-side load balancing</li>
 *   <li>Self-preservation mode (disabled in dev, enabled in staging/prod)</li>
 *   <li>Dashboard for monitoring registered instances</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>Port: 8761 (default Eureka port)</li>
 *   <li>Self-registration: disabled (standalone mode)</li>
 *   <li>Eviction interval: 5s (fast failure detection in dev)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    /**
     * Main entry point for the Discovery Service.
     * 
     * <p>Starts the Eureka Server on the configured port (8761).
     * The server will be accessible at http://localhost:8761 in dev mode.</p>
     * 
     * @param args command line arguments (profile can be set via SPRING_PROFILES_ACTIVE)
     */
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
