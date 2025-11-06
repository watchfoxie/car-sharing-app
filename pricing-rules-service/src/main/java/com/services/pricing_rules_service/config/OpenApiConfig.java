package com.services.pricing_rules_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for SpringDoc OpenAPI 3.0 documentation.
 * 
 * <p>Generates interactive REST API documentation accessible via Swagger UI.
 * This configuration is <strong>active only in {@code dev} and {@code staging} profiles</strong>,
 * and disabled in {@code prod} for security reasons.</p>
 * 
 * <p><strong>Access Points (when enabled):</strong></p>
 * <ul>
 *   <li><strong>Swagger UI</strong>: {@code http://localhost:8083/swagger-ui.html}</li>
 *   <li><strong>OpenAPI JSON</strong>: {@code http://localhost:8083/v3/api-docs}</li>
 * </ul>
 * 
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li><strong>Bearer JWT Authentication</strong>: Pre-configured security scheme for JWT tokens</li>
 *   <li><strong>RFC 7807 Problem Details</strong>: Schema definitions for standardized error responses</li>
 *   <li><strong>Grouped APIs</strong>: Endpoints organized by functional areas (pricing, management)</li>
 *   <li><strong>Rich Metadata</strong>: Detailed descriptions, examples, and validation constraints on DTOs</li>
 * </ul>
 * 
 * <p><strong>Security Integration:</strong></p>
 * <p>All endpoints are documented with the {@code bearerAuth} security scheme.
 * To test protected endpoints in Swagger UI:</p>
 * <ol>
 *   <li>Click "Authorize" button in Swagger UI</li>
 *   <li>Enter JWT token obtained from Keycloak: {@code <your-jwt-token>} (no "Bearer" prefix needed)</li>
 *   <li>Click "Authorize" to apply token to all requests</li>
 * </ol>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see org.springdoc.core.configuration.SpringDocConfiguration
 */
@Configuration
@Profile({"dev", "staging"}) // Disable OpenAPI in production
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Configures global OpenAPI metadata and security schemes.
     * 
     * <p><strong>API Information:</strong></p>
     * <ul>
     *   <li><strong>Title</strong>: Pricing Rules Service API</li>
     *   <li><strong>Version</strong>: 1.0</li>
     *   <li><strong>Description</strong>: Comprehensive pricing and tariff management for vehicle rentals</li>
     * </ul>
     * 
     * <p><strong>Security Schemes:</strong></p>
     * <ul>
     *   <li><strong>bearerAuth</strong>: HTTP Bearer token (JWT) authentication</li>
     *   <li>Format: {@code Authorization: Bearer <jwt_token>}</li>
     * </ul>
     * 
     * @return Configured OpenAPI object
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pricing Rules Service API")
                .version("1.0")
                .description("""
                    REST API for managing pricing rules and calculating rental costs in the Car Sharing platform.
                    
                    **Key Features:**
                    - Create, update, delete, and query pricing rules
                    - Calculate rental costs based on time intervals (MINUTE/HOUR/DAY)
                    - Validate rental durations against configured min/max constraints
                    - Apply operational policies (cancellation windows, late penalties)
                    
                    **Authentication:**
                    All endpoints require a valid JWT Bearer token obtained from Keycloak.
                    """)
                .contact(new Contact()
                    .name("Car Sharing Team")
                    .email("support@carsharing.example.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://carsharing.example.com/license")))
            .components(new Components()
                // Security scheme: HTTP Bearer (JWT)
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token obtained from Keycloak (no 'Bearer' prefix needed)"))
                
                // RFC 7807 Problem Details schema
                .addSchemas("ProblemDetail", new Schema<>()
                    .type("object")
                    .description("RFC 7807 Problem Details for HTTP APIs")
                    .addProperty("type", new Schema<>().type("string").example("https://carsharing.example.com/errors/validation"))
                    .addProperty("title", new Schema<>().type("string").example("Validation Error"))
                    .addProperty("status", new Schema<>().type("integer").example(422))
                    .addProperty("detail", new Schema<>().type("string").example("Price per unit must be >= 0"))
                    .addProperty("instance", new Schema<>().type("string").example("/v1/pricing/rules"))))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth")); // Apply to all endpoints
    }

    /**
     * Groups pricing-related endpoints (CRUD operations and calculations).
     * 
     * <p><strong>Included Endpoints:</strong></p>
     * <ul>
     *   <li>{@code POST /v1/pricing/calculate} - Calculate rental cost</li>
     *   <li>{@code GET /v1/pricing/rules} - List all pricing rules (paginated)</li>
     *   <li>{@code GET /v1/pricing/rules/{id}} - Get single pricing rule by ID</li>
     *   <li>{@code POST /v1/pricing/rules} - Create new pricing rule</li>
     *   <li>{@code PUT /v1/pricing/rules/{id}} - Update existing pricing rule</li>
     *   <li>{@code DELETE /v1/pricing/rules/{id}} - Delete pricing rule (soft delete)</li>
     * </ul>
     * 
     * @return Grouped OpenAPI definition for pricing endpoints
     */
    @Bean
    public GroupedOpenApi pricingApiGroup() {
        return GroupedOpenApi.builder()
            .group("pricing")
            .displayName("Pricing & Rules")
            .pathsToMatch("/v1/pricing/**")
            .build();
    }

    /**
     * Groups management/admin endpoints (Actuator health, metrics).
     * 
     * <p><strong>Included Endpoints:</strong></p>
     * <ul>
     *   <li>{@code /actuator/health} - Health check status</li>
     *   <li>{@code /actuator/info} - Application metadata</li>
     *   <li>{@code /actuator/prometheus} - Prometheus metrics (dev/staging only)</li>
     * </ul>
     * 
     * @return Grouped OpenAPI definition for management endpoints
     */
    @Bean
    public GroupedOpenApi managementApiGroup() {
        return GroupedOpenApi.builder()
            .group("management")
            .displayName("Management & Monitoring")
            .pathsToMatch("/actuator/**")
            .build();
    }
}
