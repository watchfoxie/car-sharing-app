package com.services.car_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * JPA Auditing configuration for Car Service.
 * 
 * <p>Configures automatic population of audit fields:
 * <ul>
 *   <li>{@code createdBy} - extracted from JWT 'sub' claim on INSERT</li>
 *   <li>{@code lastModifiedBy} - extracted from JWT 'sub' claim on UPDATE</li>
 *   <li>{@code createdDate} - populated automatically by @CreatedDate</li>
 *   <li>{@code lastModifiedDate} - populated automatically by @LastModifiedDate</li>
 * </ul>
 * 
 * <p>The auditor is the Keycloak subject (user ID) from the JWT token.
 * Falls back to "system" if no authentication context is available.
 * 
 * @see com.services.car_service.domain.entity.Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides the current auditor (user ID) from the JWT token.
     * 
     * <p>Extraction logic:
     * <ol>
     *   <li>Get current authentication from SecurityContext</li>
     *   <li>Extract JWT from authentication principal</li>
     *   <li>Get 'sub' claim (Keycloak subject/user ID)</li>
     *   <li>Fallback to "system" if no authentication or invalid token</li>
     * </ol>
     *
     * @return the AuditorAware bean
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
                String subject = jwt.getSubject();
                return Optional.ofNullable(subject != null ? subject : "system");
            }

            return Optional.of("system");
        };
    }
}
