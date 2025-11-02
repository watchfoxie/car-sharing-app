package com.services.car_sharing_app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for Car Sharing Application.
 * <p>
 * This configuration provides comprehensive API documentation with:
 * - Detailed metadata (title, description, version, contact, license)
 * - OAuth2/OIDC security scheme (Keycloak integration ready)
 * - Multiple server environments (dev, staging, production)
 * - Consistent API versioning and branding
 * </p>
 *
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:car-sharing-app}")
    private String applicationName;

    @Value("${springdoc.version:v1.0.0}")
    private String apiVersion;

    @Value("${springdoc.api-description:Car Sharing Application REST API}")
    private String apiDescription;

    /**
     * Configures the OpenAPI specification for the Car Sharing Application.
     * <p>
     * Includes:
     * - Application metadata and contact information
     * - OAuth2 Authorization Code Flow with PKCE (Keycloak ready)
     * - Multiple server environments
     * - Security requirements applied globally
     * </p>
     *
     * @return OpenAPI configuration bean
     */
    @Bean
    public OpenAPI carSharingOpenAPI() {
        // Security scheme name
        final String securitySchemeName = "oauth2_keycloak";

        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                .components(buildComponents(securitySchemeName))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    /**
     * Builds comprehensive API metadata.
     *
     * @return Info object with detailed API information
     */
    private Info buildApiInfo() {
        return new Info()
                .title("Car Sharing Application API")
                .description(buildDetailedDescription())
                .version(apiVersion)
                .contact(buildContact())
                .license(buildLicense())
                .termsOfService("https://car-sharing.services.com/terms");
    }

    /**
     * Builds detailed API description with business context.
     *
     * @return Detailed API description
     */
    private String buildDetailedDescription() {
        return """
                # Car Sharing Application - REST API Documentation
                
                Platforma web full-stack bazată pe microservicii pentru servicii de închiriere autovehicule (car-sharing).
                
                ## Caracteristici principale:
                
                - **Listarea flotei** - vizualizare și filtrare vehicule disponibile
                - **Configurarea tarifelor** - structură tarifară pe minute/ore/zile
                - **Căutarea rapidă** - identificare vehicule după criterii multiple
                - **Rezervarea flexibilă** - rezervări pe intervale orare/zilnice
                - **Urmărirea cicului de viață** - preluare/predare vehicule
                - **Feedback clienți** - rating și comentarii
                
                ## Arhitectură:
                
                Microservicii Spring Boot 3.5.7 cu Java 25, orchestrate prin:
                - API Gateway (Spring Cloud Gateway)
                - Discovery Service (Eureka)
                - Config Service (Spring Cloud Config)
                - Servicii business (Car, Pricing & Rules, Rental, Feedback)
                
                ## Securitate:
                
                - Autentificare: OAuth2/OIDC (Keycloak)
                - Autorizare: RBAC (Roles: ADMIN, OWNER, RENTER, VISITOR)
                - JWT tokens cu scopes și claims
                
                ## API Versioning:
                
                Toate endpoint-urile sunt versionate: `/api/v1/**`
                
                ---
                
                **Documentație generată cu SpringDoc OpenAPI v2.8.13**
                """;
    }

    /**
     * Builds contact information for API support.
     *
     * @return Contact object
     */
    private Contact buildContact() {
        return new Contact()
                .name("Car Sharing Development Team")
                .email("api-support@car-sharing.services.com")
                .url("https://car-sharing.services.com/support");
    }

    /**
     * Builds license information.
     *
     * @return License object
     */
    private License buildLicense() {
        return new License()
                .name("Proprietary License")
                .url("https://car-sharing.services.com/license");
    }

    /**
     * Configures multiple server environments.
     *
     * @return List of server configurations
     */
    private List<Server> buildServers() {
        return List.of(
                new Server()
                        .url("http://localhost:8080")
                        .description("Development Server"),
                new Server()
                        .url("https://api-staging.car-sharing.services.com")
                        .description("Staging Server"),
                new Server()
                        .url("https://api.car-sharing.services.com")
                        .description("Production Server")
        );
    }

    /**
     * Builds OpenAPI components including security schemes.
     * <p>
     * Configures OAuth2 Authorization Code Flow with PKCE for Keycloak integration.
     * Includes scopes for different roles:
     * - openid: Standard OpenID Connect scope
     * - profile: User profile information
     * - email: User email address
     * - roles: User roles (ADMIN, OWNER, RENTER, VISITOR)
     * </p>
     *
     * @param securitySchemeName Name of the security scheme
     * @return Components object with security configurations
     */
    private Components buildComponents(String securitySchemeName) {
        return new Components()
                .addSecuritySchemes(securitySchemeName, buildOAuth2SecurityScheme());
    }

    /**
     * Builds OAuth2 security scheme for Keycloak integration.
     * <p>
     * Configuration ready for Keycloak with:
     * - Authorization Code Flow with PKCE
     * - Token and authorization URLs (placeholder - to be configured per environment)
     * - Required scopes for Car Sharing application
     * </p>
     *
     * @return SecurityScheme for OAuth2
     */
    private SecurityScheme buildOAuth2SecurityScheme() {
        // Define OAuth2 scopes
        io.swagger.v3.oas.models.security.Scopes scopes = new io.swagger.v3.oas.models.security.Scopes();
        scopes.addString("openid", "OpenID Connect scope");
        scopes.addString("profile", "User profile information");
        scopes.addString("email", "User email address");
        scopes.addString("roles", "User roles (ADMIN, OWNER, RENTER, VISITOR)");

        return new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 Authorization Code Flow with PKCE (Keycloak)")
                .flows(new OAuthFlows()
                        .authorizationCode(new OAuthFlow()
                                .authorizationUrl("http://localhost:8180/realms/car-sharing/protocol/openid-connect/auth")
                                .tokenUrl("http://localhost:8180/realms/car-sharing/protocol/openid-connect/token")
                                .refreshUrl("http://localhost:8180/realms/car-sharing/protocol/openid-connect/token")
                                .scopes(scopes)
                        )
                );
    }
}
