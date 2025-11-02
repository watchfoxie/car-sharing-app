package com.services.car_sharing_app.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Health Check Controller for Car Sharing Application.
 * <p>
 * Provides basic health and status endpoints for monitoring and verification.
 * This controller demonstrates proper OpenAPI documentation annotations.
 * </p>
 *
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(
        name = "Health Check",
        description = "Endpoints for application health verification and status monitoring"
)
public class HealthController {

    /**
     * Returns the current health status of the application.
     * <p>
     * This endpoint can be used by load balancers, monitoring tools,
     * and orchestration platforms to verify service availability.
     * </p>
     *
     * @return ResponseEntity containing HealthStatus
     */
    @Operation(
            summary = "Get application health status",
            description = """
                    Returns the current health and operational status of the Car Sharing Application.
                    
                    **Use cases:**
                    - Load balancer health checks
                    - Monitoring and alerting systems
                    - Service mesh health probes
                    - Deployment verification
                    
                    **Status codes:**
                    - 200 OK: Service is healthy and operational
                    - 503 Service Unavailable: Service is degraded or unavailable
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy and operational",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = HealthStatus.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Service is degraded or unavailable",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthStatus> getHealth() {
        HealthStatus status = new HealthStatus(
                "UP",
                "car-sharing-app",
                "1.0.0",
                Instant.now(),
                "Service is healthy and operational"
        );
        return ResponseEntity.ok(status);
    }

    /**
     * Returns basic application information.
     * <p>
     * Provides metadata about the application version, name, and build details.
     * Useful for version verification and troubleshooting.
     * </p>
     *
     * @return ResponseEntity containing AppInfo
     */
    @Operation(
            summary = "Get application information",
            description = """
                    Returns basic metadata about the Car Sharing Application.
                    
                    **Includes:**
                    - Application name and version
                    - Build timestamp
                    - Spring Boot version
                    - Java version
                    - Active profiles
                    
                    **Use cases:**
                    - Version verification
                    - Deployment validation
                    - Troubleshooting and support
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Application information retrieved successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AppInfo.class)
                    )
            )
    })
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AppInfo> getInfo() {
        AppInfo info = new AppInfo(
                "car-sharing-app",
                "1.0.0",
                "Car Sharing Application - Vehicle Rental Service Platform",
                "Spring Boot 3.5.7",
                "Java 25",
                "microservices"
        );
        return ResponseEntity.ok(info);
    }

    /**
     * Data Transfer Object for Health Status.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "HealthStatus",
            description = "Health status information for the application"
    )
    public static class HealthStatus {
        @Schema(
                description = "Current health status",
                example = "UP",
                allowableValues = {"UP", "DOWN", "DEGRADED"}
        )
        private String status;

        @Schema(description = "Application name", example = "car-sharing-app")
        private String application;

        @Schema(description = "Application version", example = "1.0.0")
        private String version;

        @Schema(description = "Current server timestamp", example = "2025-11-02T10:15:30.123Z")
        private Instant timestamp;

        @Schema(description = "Detailed health message", example = "Service is healthy and operational")
        private String message;
    }

    /**
     * Data Transfer Object for Application Information.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(
            name = "AppInfo",
            description = "Application metadata and build information"
    )
    public static class AppInfo {
        @Schema(description = "Application name", example = "car-sharing-app")
        private String name;

        @Schema(description = "Application version", example = "1.0.0")
        private String version;

        @Schema(description = "Application description")
        private String description;

        @Schema(description = "Spring Boot version", example = "3.5.7")
        private String springBootVersion;

        @Schema(description = "Java version", example = "Java 25")
        private String javaVersion;

        @Schema(description = "Architecture type", example = "microservices")
        private String architecture;
    }
}
