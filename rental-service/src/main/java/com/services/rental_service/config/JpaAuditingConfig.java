package com.services.rental_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * JPA Auditing configuration for Rental Service.
 * <p>
 * Configures automatic population of audit fields ({@code createdBy}, {@code lastModifiedBy})
 * by extracting the user ID from the JWT {@code sub} claim in the security context.
 * </p>
 * <p>
 * Fallback behavior:
 * <ul>
 *   <li>If no authentication is present → "system"</li>
 *   <li>If authentication principal is not a JWT → "system"</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * AuditorAware bean that extracts the current user ID from JWT.
     * <p>
     * Extracts the {@code sub} (subject) claim from the JWT principal.
     * This claim typically contains the unique user ID from Keycloak.
     * </p>
     *
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                // Extract 'sub' claim (user ID) from JWT
                String subject = jwt.getSubject();
                return Optional.ofNullable(subject).or(() -> Optional.of("system"));
            }

            // Fallback for non-JWT authentication (e.g., tests with mock users)
            return Optional.of("system");
        };
    }
}
