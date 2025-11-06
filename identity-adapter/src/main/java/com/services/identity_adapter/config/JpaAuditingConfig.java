package com.services.identity_adapter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * JPA Auditing configuration for automatic population of audit fields.
 * 
 * <p>Enables automatic population of {@code @CreatedBy} and {@code @LastModifiedBy} fields
 * with the current authenticated user's ID (from JWT sub claim).
 * 
 * <p><strong>Audit fields populated:</strong>
 * <ul>
 *   <li>createdDate: automatically set on INSERT</li>
 *   <li>createdBy: extracted from JWT sub claim on INSERT</li>
 *   <li>lastModifiedDate: automatically updated on UPDATE</li>
 *   <li>lastModifiedBy: extracted from JWT sub claim on UPDATE</li>
 * </ul>
 * 
 * <p><strong>Fallback behavior:</strong>
 * If no authenticated user is found (e.g., system operations), falls back to "system".
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides current auditor (user ID from JWT sub claim).
     * 
     * <p>Extracts the {@code sub} claim from JWT token in SecurityContext.
     * Falls back to "system" if no authentication is present.
     * 
     * @return AuditorAware bean for JPA auditing
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }
            
            // Extract subject (user ID) from JWT token
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt jwt) {
                String subject = jwt.getSubject();
                return Optional.ofNullable(subject);
            }
            
            // Fallback to authentication name or "system"
            String name = authentication.getName();
            return Optional.ofNullable(name != null ? name : "system");
        };
    }
}
