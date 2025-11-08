package com.services.feedback_service.config;

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
 * <p>Extracts user ID from JWT {@code sub} claim for:
 * <ul>
 *   <li>{@code @CreatedBy}: Set on entity creation</li>
 *   <li>{@code @LastModifiedBy}: Updated on entity modification</li>
 * </ul>
 * 
 * <p>Fallback:
 * <ul>
 *   <li>If no authentication: returns {@code "system"}</li>
 *   <li>If JWT sub claim missing: returns {@code "unknown"}</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    /**
     * Provides current auditor (user ID) from JWT authentication.
     * 
     * @return auditor aware bean
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system");
            }

            if (authentication.getPrincipal() instanceof Jwt jwt) {
                String sub = jwt.getSubject();
                return Optional.ofNullable(sub != null ? sub : "unknown");
            }

            return Optional.of("system");
        };
    }
}
