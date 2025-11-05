package com.services.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API Gateway Application - Spring Cloud Gateway (Reactive)
 * 
 * <p>Central entry point (front-door) for the Car Sharing microservices architecture.
 * Routes all client requests to appropriate backend services with support for
 * cross-cutting concerns: security, rate limiting, circuit breaking, and observability.</p>
 * 
 * <p>Key Responsibilities:</p>
 * <ul>
 *   <li>Request routing to microservices via service discovery (lb://service-name)</li>
 *   <li>OAuth2 JWT validation and authentication</li>
 *   <li>CORS handling for Angular frontend (http://localhost:4200 in dev)</li>
 *   <li>Rate limiting per client (Redis-backed in staging/prod)</li>
 *   <li>Circuit breaking and timeouts for resilience (Resilience4j)</li>
 *   <li>HTTP caching (ETag/If-None-Match)</li>
 *   <li>OpenAPI aggregation from all services (dev mode only)</li>
 *   <li>Distributed tracing and metrics export</li>
 * </ul>
 * 
 * <p>Architecture Pattern:</p>
 * <pre>
 * Angular App (4200)
 *      ↓
 * API Gateway (8080) ← JWT validation, CORS, Rate Limit
 *      ↓ (lb://)
 * Service Discovery (8761)
 *      ↓
 * Microservices (8081-8085)
 * </pre>
 * 
 * <p>Routing Strategy:</p>
 * <ul>
 *   <li>Dynamic discovery: Gateway automatically routes to services registered in Eureka</li>
 *   <li>Path-based routing: /api/v1/cars → lb://car-service/v1/cars</li>
 *   <li>Explicit routes for infrastructure services (Eureka dashboard, Config Server)</li>
 * </ul>
 * 
 * <p>Security:</p>
 * <ul>
 *   <li>OAuth2 Resource Server: Validates JWT tokens from Keycloak</li>
 *   <li>Role-based access: Forwards authenticated principal to downstream services</li>
 *   <li>Public endpoints: /actuator/health, /eureka (dashboard in dev)</li>
 *   <li>CORS: Strict whitelist (localhost:4200 in dev, prod domains in prod)</li>
 * </ul>
 * 
 * <p>Resilience:</p>
 * <ul>
 *   <li>Circuit Breaker: Prevent cascade failures (Resilience4j)</li>
 *   <li>Timeouts: Aggressive timeouts per route (5s default, configurable)</li>
 *   <li>Retry: Exponential backoff with jitter for transient failures</li>
 *   <li>Bulkhead: Resource isolation between routes</li>
 * </ul>
 * 
 * <p>Observability:</p>
 * <ul>
 *   <li>Metrics: Micrometer → Prometheus (request count, latency, errors)</li>
 *   <li>Tracing: Spring Cloud Sleuth → Zipkin (distributed trace correlation)</li>
 *   <li>Logging: Structured JSON logs with traceId/spanId correlation</li>
 *   <li>Health: Actuator aggregates health of all routes and dependencies</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>Port: 8080</li>
 *   <li>Discovery: Eureka Client (registers with discovery-service:8761)</li>
 *   <li>Config: Spring Cloud Config Client (loads from config-service:8888)</li>
 *   <li>Profiles: dev (permissive), staging, prod (strict)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    /**
     * Main entry point for the API Gateway.
     * 
     * <p>Startup sequence:</p>
     * <ol>
     *   <li>Connects to Config Server (8888) to load configuration</li>
     *   <li>Registers with Eureka Discovery (8761)</li>
     *   <li>Initializes reactive routes from configuration</li>
     *   <li>Starts Netty web server on port 8080</li>
     * </ol>
     * 
     * <p>Required dependencies:</p>
     * <ul>
     *   <li>Discovery Service (8761) - Must be running for service discovery</li>
     *   <li>Config Service (8888) - Optional, falls back to local application.yaml</li>
     *   <li>Redis (6379) - Optional in dev, required for rate limiting in staging/prod</li>
     * </ul>
     * 
     * @param args command line arguments (profile via SPRING_PROFILES_ACTIVE)
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
