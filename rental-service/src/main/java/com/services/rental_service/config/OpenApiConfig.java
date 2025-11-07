package com.services.rental_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SpringDoc OpenAPI 3.0 configuration for Rental Service.
 * <p>
 * Generates interactive REST API documentation accessible at:
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8084/swagger-ui.html">http://localhost:8084/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="http://localhost:8084/v3/api-docs">http://localhost:8084/v3/api-docs</a></li>
 * </ul>
 * </p>
 * <p>
 * <strong>Security:</strong> Configured with HTTP Bearer JWT authentication scheme.
 * Users must obtain a JWT token from Keycloak and include it in the {@code Authorization} header.
 * </p>
 * <p>
 * <strong>Active profiles:</strong> {@code dev}, {@code staging} (disabled in {@code prod})
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Configuration
@Profile({"dev", "staging"})
public class OpenApiConfig {

    /**
     * Configure OpenAPI metadata and security schemes.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI rentalServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rental Service API")
                        .version("1.0")
                        .description("""
                                RESTful API for managing the complete rental lifecycle in the Car Sharing platform.
                                
                                **Key Features:**
                                - Conflict-free rental booking with EXCLUDE constraint
                                - Finite State Machine (FSM): PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED
                                - Idempotency support via (renter_id, idempotency_key)
                                - Cost estimation and final cost calculation
                                - Operator return approval workflow
                                
                                **Authentication:**
                                All endpoints (except health checks) require JWT Bearer token from Keycloak.
                                Include token in `Authorization: Bearer <token>` header.
                                
                                **Error Handling:**
                                All errors follow RFC 7807 Problem Details format (`application/problem+json`).
                                """)
                        .contact(new Contact()
                                .name("Car Sharing Team")
                                .email("support@carsharing.example.com")
                                .url("https://github.com/watchfoxie/car-sharing-app"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
                .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
                .components(new Components()
                        .addSecuritySchemes("Bearer JWT", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("""
                                        JWT token issued by Keycloak.
                                        
                                        **How to obtain:**
                                        1. Authenticate with Keycloak using OAuth2 flow
                                        2. Extract `access_token` from response
                                        3. Include token in `Authorization: Bearer <access_token>` header
                                        
                                        **Token claims:**
                                        - `sub`: User ID (extracted for audit trail)
                                        - `realm_access.roles`: User roles (RENTER, OWNER, ADMIN)
                                        """)
                        )
                        // Add RFC 7807 Problem Details schema
                        .addSchemas("ProblemDetail", new Schema<>()
                                .type("object")
                                .description("RFC 7807 Problem Details for HTTP APIs")
                                .addProperty("type", new Schema<>().type("string").example("about:blank"))
                                .addProperty("title", new Schema<>().type("string").example("Bad Request"))
                                .addProperty("status", new Schema<>().type("integer").example(400))
                                .addProperty("detail", new Schema<>().type("string").example("Validation failed for field 'pickupDatetime'"))
                                .addProperty("instance", new Schema<>().type("string").example("/v1/rentals"))
                                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                                .addProperty("traceId", new Schema<>().type("string").example("abc123xyz"))
                        )
                );
    }

    /**
     * Group API endpoints by version (v1).
     *
     * @return Grouped API configuration
     */
    @Bean
    public GroupedOpenApi rentalApiV1() {
        return GroupedOpenApi.builder()
                .group("v1-rentals")
                .pathsToMatch("/v1/rentals/**")
                .build();
    }

    /**
     * Group actuator endpoints separately.
     *
     * @return Grouped API configuration for actuator
     */
    @Bean
    public GroupedOpenApi actuatorApi() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}
