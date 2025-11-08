package com.services.feedback_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI 3.0 configuration for Feedback Service API documentation.
 * 
 * <p>Generates interactive Swagger UI and OpenAPI JSON specification.
 * 
 * <p>Available at:
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8085/swagger-ui.html">http://localhost:8085/swagger-ui.html</a></li>
 *   <li>OpenAPI JSON: <a href="http://localhost:8085/v3/api-docs">http://localhost:8085/v3/api-docs</a></li>
 * </ul>
 * 
 * <p>Active profiles: {@code dev}, {@code staging} (disabled in {@code prod})
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Configuration
@Profile({"dev", "staging"})
public class OpenApiConfig {

    /**
     * Configures OpenAPI specification with security scheme and error schemas.
     * 
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI feedbackServiceOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Feedback Service API")
                .version("1.0")
                .description("""
                    REST API for managing customer feedback in the car-sharing platform.
                    
                    ## Features
                    - Submit feedback for completed rentals
                    - View aggregated ratings and comments per car
                    - Anti-abuse policies (duplicate prevention, rate limiting)
                    - Reports and analytics (top cars, rating distribution)
                    
                    ## Base URL
                    - Local: http://localhost:8085
                    - Via API Gateway: http://localhost:8080/api
                    
                    ## Authentication
                    All write endpoints require JWT Bearer token from Keycloak.
                    Read endpoints (aggregations) are public.
                    """)
                .contact(new Contact()
                    .name("Car Sharing Team")
                    .email("support@carsharing.example.com")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Keycloak (realm: car-sharing)")
                )
                // RFC 7807 Problem Details schema
                .addSchemas("ProblemDetail", new Schema<>()
                    .type("object")
                    .description("RFC 7807 Problem Details for HTTP APIs")
                    .addProperty("type", new Schema<>().type("string").example("about:blank"))
                    .addProperty("title", new Schema<>().type("string").example("Bad Request"))
                    .addProperty("status", new Schema<>().type("integer").example(400))
                    .addProperty("detail", new Schema<>().type("string").example("Validation failed"))
                    .addProperty("instance", new Schema<>().type("string").example("/v1/feedback"))
                    .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                )
            );
    }
}
