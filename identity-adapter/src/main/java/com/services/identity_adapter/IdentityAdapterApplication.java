package com.services.identity_adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Identity Adapter Service - Main Application.
 * 
 * <p>This service manages user identities and integrates with external OIDC providers (Keycloak).
 * It handles account profiles, role mappings, and authentication/authorization workflows.
 * 
 * <p><strong>Key responsibilities:</strong>
 * <ul>
 *   <li>Integration with Keycloak/OIDC for SSO authentication</li>
 *   <li>Management of {@code identity.accounts} table with user profiles</li>
 *   <li>Role mapping (ADMIN, OWNER, RENTER) from JWT tokens</li>
 *   <li>OAuth2 Resource Server for JWT validation</li>
 *   <li>User profile endpoints (read/update non-sensitive attributes)</li>
 * </ul>
 * 
 * <p><strong>Database schema:</strong> {@code identity} (PostgreSQL)
 * 
 * <p><strong>Flyway migrations:</strong> {@code V1__init.sql} creates accounts table with citext support
 * 
 * <p><strong>Security:</strong> OAuth2 Resource Server with JWT validation, RBAC on endpoints
 * 
 * <p><strong>Note:</strong> JPA Auditing is enabled via {@link com.services.identity_adapter.config.JpaAuditingConfig}
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@SpringBootApplication
@EnableDiscoveryClient
public class IdentityAdapterApplication {

    /**
     * Main entry point for Identity Adapter Service.
     * 
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(IdentityAdapterApplication.class, args);
    }
}
