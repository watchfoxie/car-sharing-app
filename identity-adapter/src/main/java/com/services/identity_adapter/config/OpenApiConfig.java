package com.services.identity_adapter.config;

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
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Identity Adapter Service.
 * 
 * <p>Configures Swagger UI and OpenAPI documentation with:
 * <ul>
 *   <li>Service metadata (title, version, description)</li>
 *   <li>OAuth2 JWT Bearer security scheme</li>
 *   <li>RFC 7807 Problem Details error responses</li>
 *   <li>Server URLs for different environments</li>
 * </ul>
 * 
 * <p><strong>Active profiles:</strong> dev, staging (disabled in prod)
 * 
 * <p><strong>Access:</strong>
 * <ul>
 *   <li>Swagger UI: http://localhost:8081/swagger-ui.html</li>
 *   <li>OpenAPI JSON: http://localhost:8081/v3/api-docs</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Configuration
@Profile({"dev", "staging"})
public class OpenApiConfig {

    @Value("${spring.application.name:identity-adapter}")
    private String applicationName;

    @Value("${server.port:8081}")
    private String serverPort;

    /**
     * Configures OpenAPI specification with security and error schemas.
     * 
     * @return configured OpenAPI bean
     */
    @Bean
    public OpenAPI identityAdapterOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .servers(serverList())
            .addSecurityItem(securityRequirement())
            .components(apiComponents());
    }

    /**
     * API metadata information.
     */
    private Info apiInfo() {
        return new Info()
            .title("Identity Adapter Service API")
            .description("""
                Identity and Access Management service for Car Sharing platform.
                
                Provides:
                - User account management
                - OAuth2/OIDC integration with Keycloak
                - Role-based access control (ADMIN, OWNER, RENTER)
                - Profile management endpoints
                
                Security: All endpoints require valid JWT Bearer token from Keycloak.
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("Car Sharing Development Team")
                .email("dev@carsharing.example.com"))
            .license(new License()
                .name("Proprietary")
                .url("https://carsharing.example.com/license"));
    }

    /**
     * Server URLs for different environments.
     */
    private List<Server> serverList() {
        return List.of(
            new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server"),
            new Server()
                .url("http://localhost:8080/api")
                .description("Via API Gateway (recommended)")
        );
    }

    /**
     * Global security requirement (JWT Bearer token).
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }

    /**
     * API components including security schemes and reusable schemas.
     */
    private Components apiComponents() {
        return new Components()
            .addSecuritySchemes("bearerAuth", bearerAuthScheme())
            .addSchemas("ProblemDetail", problemDetailSchema())
            .addResponses("Unauthorized", unauthorizedResponse())
            .addResponses("Forbidden", forbiddenResponse())
            .addResponses("NotFound", notFoundResponse())
            .addResponses("ValidationError", validationErrorResponse())
            .addResponses("InternalServerError", internalServerErrorResponse());
    }

    /**
     * JWT Bearer authentication security scheme.
     */
    private SecurityScheme bearerAuthScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT token from Keycloak OIDC provider. Format: `Bearer <token>`");
    }

    /**
     * RFC 7807 Problem Detail schema for error responses.
     */
    @SuppressWarnings("rawtypes")
    private Schema problemDetailSchema() {
        return new Schema()
            .type("object")
            .description("RFC 7807 Problem Details for HTTP APIs")
            .addProperty("type", new Schema().type("string").description("Problem type URI"))
            .addProperty("title", new Schema().type("string").description("Short summary"))
            .addProperty("status", new Schema().type("integer").description("HTTP status code"))
            .addProperty("detail", new Schema().type("string").description("Detailed explanation"))
            .addProperty("instance", new Schema().type("string").description("URI reference to specific occurrence"))
            .addProperty("timestamp", new Schema().type("string").format("date-time"));
    }

    /**
     * 401 Unauthorized response.
     */
    private ApiResponse unauthorizedResponse() {
        return new ApiResponse()
            .description("Authentication required or token invalid")
            .content(problemDetailContent());
    }

    /**
     * 403 Forbidden response.
     */
    private ApiResponse forbiddenResponse() {
        return new ApiResponse()
            .description("Insufficient privileges for this operation")
            .content(problemDetailContent());
    }

    /**
     * 404 Not Found response.
     */
    private ApiResponse notFoundResponse() {
        return new ApiResponse()
            .description("Requested resource not found")
            .content(problemDetailContent());
    }

    /**
     * 422 Validation Error response.
     */
    private ApiResponse validationErrorResponse() {
        return new ApiResponse()
            .description("Request validation failed")
            .content(problemDetailContent());
    }

    /**
     * 500 Internal Server Error response.
     */
    private ApiResponse internalServerErrorResponse() {
        return new ApiResponse()
            .description("Internal server error occurred")
            .content(problemDetailContent());
    }

    /**
     * Problem Detail content (application/problem+json).
     */
    @SuppressWarnings("rawtypes")
    private Content problemDetailContent() {
        return new Content()
            .addMediaType("application/problem+json",
                new MediaType().schema(new Schema().$ref("#/components/schemas/ProblemDetail")));
    }
}
