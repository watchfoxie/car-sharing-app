package com.services.rental.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

/**
 * Custom health indicator for Keycloak OIDC issuer availability.
 * Validates that the OAuth2 Resource Server can reach the Keycloak issuer
 * for JWT validation and public key retrieval.
 * 
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html">JWT Resource Server</a>
 */
@Slf4j
@Component
public class KeycloakHealthIndicator implements HealthIndicator {
    
    private final RestClient restClient;
    private final URI issuerUri;
    private final URI wellKnownUri;
    
    public KeycloakHealthIndicator(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        this.issuerUri = URI.create(issuerUri);
        this.wellKnownUri = URI.create(issuerUri + "/.well-known/openid-configuration");
        this.restClient = RestClient.builder()
                .baseUrl(issuerUri)
                .build();
    }
    
    @Override
    public Health health() {
        try {
            // Attempt to fetch OIDC discovery document
            String response = restClient.get()
                    .uri(wellKnownUri)
                    .retrieve()
                    .body(String.class);
            
            if (response != null && response.contains("issuer")) {
                log.trace("Keycloak health check successful: issuerUri={}", issuerUri);
                return Health.up()
                        .withDetail("issuerUri", issuerUri.toString())
                        .withDetail("wellKnownUri", wellKnownUri.toString())
                        .build();
            } else {
                log.warn("Keycloak health check failed: invalid OIDC discovery response");
                return Health.down()
                        .withDetail("error", "InvalidOIDCResponse")
                        .withDetail("message", "OIDC discovery document does not contain issuer field")
                        .build();
            }
        } catch (Exception e) {
            log.error("Keycloak health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .withDetail("issuerUri", issuerUri.toString())
                    .build();
        }
    }
}
