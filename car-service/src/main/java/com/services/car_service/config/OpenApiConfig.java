package com.services.car_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI 3.0 configuration for Car Service.
 * 
 * <p>Active profiles: dev, staging (disabled in prod for security).
 * 
 * <p>Features:
 * <ul>
 *   <li>Bearer JWT authentication scheme</li>
 *   <li>RFC 7807 Problem Details schema for errors</li>
 *   <li>API metadata (title, version, description, contact)</li>
 *   <li>Global security requirement (JWT Bearer)</li>
 * </ul>
 * 
 * <p>Access:
 * <ul>
 *   <li>Swagger UI: http://localhost:8082/swagger-ui.html</li>
 *   <li>OpenAPI JSON: http://localhost:8082/v3/api-docs</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@Profile({"dev", "staging"})
public class OpenApiConfig {

    /**
     * Configures OpenAPI documentation for Car Service.
     *
     * @return the OpenAPI bean
     */
    @Bean
    public OpenAPI carServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Car Service API")
                .version("1.0")
                .description("""
                    Microservice for managing the vehicle fleet in the Car Sharing platform.
                    
                    **Features:**
                    - CRUD operations for cars (owner-based access control)
                    - Advanced search and filtering (brand, category, price)
                    - Sorting (A-Z/Z-A by brand, price ascending/descending)
                    - Redis caching for public listings
                    - Integration with Feedback Service for ratings
                    
                    **Authentication:**
                    All endpoints require JWT Bearer token from Keycloak.
                    """)
                .contact(new Contact()
                    .name("Car Sharing Team")
                    .email("support@carsharing.example.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://carsharing.example.com/license")))
            .components(new Components()
                // JWT Bearer authentication scheme
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Keycloak (obtained via OAuth2/OIDC flow)"))
                // RFC 7807 Problem Details schema
                .addSchemas("ProblemDetail", new Schema<>()
                    .type("object")
                    .description("RFC 7807 Problem Details for error responses")
                    .addProperty("type", new Schema<>().type("string").example("about:blank"))
                    .addProperty("title", new Schema<>().type("string").example("Bad Request"))
                    .addProperty("status", new Schema<>().type("integer").example(400))
                    .addProperty("detail", new Schema<>().type("string").example("Validation failed for field 'brand'"))
                    .addProperty("instance", new Schema<>().type("string").example("/v1/cars/123")))
                // Global error responses
                .addResponses("Unauthorized", new ApiResponse()
                    .description("Unauthorized - missing or invalid JWT token")
                    .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("Forbidden", new ApiResponse()
                    .description("Forbidden - insufficient permissions")
                    .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("NotFound", new ApiResponse()
                    .description("Not Found - resource does not exist")
                    .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
                .addResponses("ValidationError", new ApiResponse()
                    .description("Unprocessable Entity - validation failed")
                    .content(new Content().addMediaType("application/problem+json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ProblemDetail")))))
            )
            // Apply JWT authentication globally
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
