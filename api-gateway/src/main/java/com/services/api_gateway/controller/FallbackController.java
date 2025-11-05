package com.services.api_gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker
 * 
 * <p>Provides graceful degradation responses when backend services are unavailable
 * or circuit breakers are open. Returns standardized error responses conforming
 * to RFC 7807 (application/problem+json).</p>
 * 
 * <p>Triggered by:</p>
 * <ul>
 *   <li>Circuit breaker OPEN state (too many failures)</li>
 *   <li>Timeout exceeded (default 5s)</li>
 *   <li>Service unavailable (not registered in Eureka)</li>
 * </ul>
 * 
 * <p>Response format (RFC 7807):</p>
 * <pre>
 * {
 *   "type": "https://car-sharing.com/errors/service-unavailable",
 *   "title": "Service Temporarily Unavailable",
 *   "status": 503,
 *   "detail": "The car-service is currently unavailable. Please try again later.",
 *   "instance": "/api/v1/cars/123",
 *   "timestamp": "2025-11-05T10:30:00Z",
 *   "serviceName": "car-service"
 * }
 * </pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-05
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Fallback for Car Service failures.
     * 
     * @return RFC 7807 problem detail response
     */
    @GetMapping("/car-service")
    public ResponseEntity<Map<String, Object>> carServiceFallback() {
        return buildFallbackResponse("car-service", 
            "The car listing service is temporarily unavailable. Please try again in a few moments.");
    }

    /**
     * Fallback for Pricing Service failures.
     * 
     * @return RFC 7807 problem detail response
     */
    @GetMapping("/pricing-service")
    public ResponseEntity<Map<String, Object>> pricingServiceFallback() {
        return buildFallbackResponse("pricing-rules-service", 
            "The pricing calculation service is temporarily unavailable. Please try again later.");
    }

    /**
     * Fallback for Rental Service failures.
     * 
     * @return RFC 7807 problem detail response
     */
    @GetMapping("/rental-service")
    public ResponseEntity<Map<String, Object>> rentalServiceFallback() {
        return buildFallbackResponse("rental-service", 
            "The rental booking service is temporarily unavailable. Your data is safe, please try again later.");
    }

    /**
     * Fallback for Feedback Service failures.
     * 
     * @return RFC 7807 problem detail response
     */
    @GetMapping("/feedback-service")
    public ResponseEntity<Map<String, Object>> feedbackServiceFallback() {
        return buildFallbackResponse("feedback-service", 
            "The feedback service is temporarily unavailable. Your feedback will be saved when the service is restored.");
    }

    /**
     * Fallback for Identity Service failures.
     * 
     * @return RFC 7807 problem detail response
     */
    @GetMapping("/identity-service")
    public ResponseEntity<Map<String, Object>> identityServiceFallback() {
        return buildFallbackResponse("identity-adapter", 
            "The user account service is temporarily unavailable. Please try again later.");
    }

    /**
     * Builds standardized RFC 7807 problem detail response.
     * 
     * @param serviceName Name of the unavailable service
     * @param detail Human-readable error description
     * @return ResponseEntity with 503 Service Unavailable status
     */
    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName, String detail) {
        Map<String, Object> problemDetail = new HashMap<>();
        problemDetail.put("type", "https://car-sharing.com/errors/service-unavailable");
        problemDetail.put("title", "Service Temporarily Unavailable");
        problemDetail.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        problemDetail.put("detail", detail);
        problemDetail.put("timestamp", Instant.now().toString());
        problemDetail.put("serviceName", serviceName);
        
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Content-Type", "application/problem+json")
            .body(problemDetail);
    }
}
