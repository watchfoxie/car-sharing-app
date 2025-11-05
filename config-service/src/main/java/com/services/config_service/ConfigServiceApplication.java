package com.services.config_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Config Service Application - Spring Cloud Config Server
 * 
 * <p>Centralized configuration management server for the Car Sharing microservices architecture.
 * Provides externalized configuration across all environments (dev, staging, prod) with
 * support for Git-based versioning and dynamic refresh capabilities.</p>
 * 
 * <p>Key Features:</p>
 * <ul>
 *   <li>Git-backed configuration storage with version control</li>
 *   <li>Environment-specific property files ({service}-{profile}.yaml)</li>
 *   <li>Dynamic configuration refresh via Spring Cloud Bus (planned)</li>
 *   <li>Placeholder for HashiCorp Vault integration (secrets management)</li>
 *   <li>Fallback to local file system in dev mode</li>
 * </ul>
 * 
 * <p>Configuration Structure:</p>
 * <pre>
 * config-repo/
 * ├── api-gateway.yaml
 * ├── api-gateway-dev.yaml
 * ├── api-gateway-prod.yaml
 * ├── car-service.yaml
 * ├── car-service-dev.yaml
 * └── ...
 * </pre>
 * 
 * <p>Access Pattern:</p>
 * <ul>
 *   <li>/{application}/{profile}[/{label}]</li>
 *   <li>/{application}-{profile}.yaml</li>
 *   <li>/{label}/{application}-{profile}.yaml</li>
 * </ul>
 * 
 * <p>Example:</p>
 * <pre>
 * GET http://localhost:8888/car-service/dev
 * GET http://localhost:8888/car-service-dev.yaml
 * </pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServiceApplication {

    /**
     * Main entry point for the Config Service.
     * 
     * <p>Starts the Spring Cloud Config Server on port 8888.
     * The server clones the Git repository on startup (clone-on-start: true)
     * and registers itself with Eureka for discovery.</p>
     * 
     * <p>Configuration sources (priority order):</p>
     * <ol>
     *   <li>Git repository (default: file://${user.home}/car-sharing-config)</li>
     *   <li>Native file system (dev fallback: ./config-repo)</li>
     *   <li>Classpath:/config (embedded defaults)</li>
     * </ol>
     * 
     * @param args command line arguments (profile via SPRING_PROFILES_ACTIVE)
     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServiceApplication.class, args);
    }
}
