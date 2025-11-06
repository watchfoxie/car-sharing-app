package com.services.pricing_rules_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Configuration for JPA Auditing functionality.
 * 
 * <p>Enables automatic population of audit fields in JPA entities:</p>
 * <ul>
 *   <li>{@code @CreatedDate} - Timestamp when entity was created</li>
 *   <li>{@code @LastModifiedDate} - Timestamp when entity was last modified</li>
 *   <li>{@code @CreatedBy} - Account ID of user who created the entity</li>
 *   <li>{@code @LastModifiedBy} - Account ID of user who last modified the entity</li>
 * </ul>
 * 
 * <p><strong>Audit Actor Extraction:</strong></p>
 * <ul>
 *   <li>Extracts the {@code sub} (subject) claim from the JWT token in the current security context</li>
 *   <li>The {@code sub} claim contains the Keycloak user ID (account ID)</li>
 *   <li>Falls back to {@code "system"} for non-authenticated operations (e.g., scheduled jobs, migrations)</li>
 * </ul>
 * 
 * <p><strong>Usage in Entities:</strong></p>
 * <pre>{@code
 * @Entity
 * @EntityListeners(AuditingEntityListener.class)
 * public class PricingRule {
 *     @CreatedDate
 *     private Instant createdDate; // Auto-populated on persist
 *     
 *     @LastModifiedDate
 *     private Instant lastModifiedDate; // Auto-updated on merge
 *     
 *     @CreatedBy
 *     private String createdBy; // Account ID from JWT sub claim
 *     
 *     @LastModifiedBy
 *     private String lastModifiedBy; // Account ID from JWT sub claim
 * }
 * }</pre>
 * 
 * <p><strong>JWT Token Example:</strong></p>
 * <pre>{@code
 * {
 *   "sub": "keycloak-user-id-123", // This value is extracted as auditor
 *   "email": "user@example.com",
 *   "realm_access": { "roles": ["OWNER"] },
 *   ...
 * }
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see org.springframework.data.jpa.repository.config.EnableJpaAuditing
 * @see org.springframework.data.domain.AuditorAware
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /**
     * Provides the current auditor (user performing the operation) for JPA auditing.
     * 
     * <p><strong>Logic:</strong></p>
     * <ol>
     *   <li>Attempt to retrieve the current Spring Security {@code Authentication} from {@code SecurityContextHolder}</li>
     *   <li>If authentication exists and principal is a {@code Jwt}, extract the {@code sub} claim (Keycloak user ID)</li>
     *   <li>If no authentication or not a JWT, return {@code "system"} (fallback for scheduled jobs, migrations, etc.)</li>
     * </ol>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <ul>
     *   <li>{@code SecurityContextHolder} is thread-local, so this method is safe for concurrent requests</li>
     * </ul>
     * 
     * @return AuditorAware bean that provides the current user's account ID
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                // No authentication context (e.g., scheduled jobs, Flyway migrations)
                return Optional.of("system");
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof Jwt jwt) {
                // Extract 'sub' claim from JWT (Keycloak user ID)
                String subject = jwt.getSubject(); // Equivalent to jwt.getClaim("sub")
                return Optional.ofNullable(subject).or(() -> Optional.of("system"));
            }

            // Authentication exists but principal is not a JWT (unlikely in OAuth2 Resource Server)
            return Optional.of("system");
        };
    }
}
