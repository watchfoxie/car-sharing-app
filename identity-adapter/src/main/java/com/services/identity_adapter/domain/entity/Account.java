package com.services.identity_adapter.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Account entity representing user profiles in the Car Sharing platform.
 * 
 * <p>Maps to {@code identity.accounts} table in PostgreSQL. This entity stores user
 * information synchronized with external OIDC provider (Keycloak) and local profile data.
 * 
 * <p><strong>Key features:</strong>
 * <ul>
 *   <li>Primary key {@code id} stores external subject/ID from Keycloak</li>
 *   <li>{@code username} and {@code email} use citext for case-insensitive uniqueness</li>
 *   <li>Full audit trail with created/modified dates and actors</li>
 *   <li>Soft delete support via {@code enabled} flag</li>
 * </ul>
 * 
 * <p><strong>Database constraints:</strong>
 * <ul>
 *   <li>username: UNIQUE, NOT NULL, case-insensitive (citext)</li>
 *   <li>email: UNIQUE (allows multiple NULLs), case-insensitive (citext)</li>
 *   <li>Audit trigger: {@code public.set_audit_fields()}</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Entity
@Table(name = "accounts", schema = "identity", indexes = {
    @Index(name = "uq_identity_accounts_email", columnList = "email", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"createdBy", "lastModifiedBy"})
@EqualsAndHashCode(of = "id")
public class Account {

    /**
     * External subject ID from OIDC provider (e.g., Keycloak sub claim).
     * 
     * <p>This is the primary key and should match the {@code sub} claim from JWT tokens.
     */
    @Id
    @Column(name = "id", length = 255, nullable = false)
    @NotBlank(message = "Account ID cannot be blank")
    @Size(max = 255, message = "Account ID must not exceed 255 characters")
    private String id;

    /**
     * Unique username (case-insensitive via citext).
     * 
     * <p>Used for login and display. Synchronized with OIDC provider.
     */
    @Column(name = "username", nullable = false, unique = true, columnDefinition = "citext")
    @NotBlank(message = "Username is required")
    @Size(max = 255, message = "Username must not exceed 255 characters")
    private String username;

    /**
     * User email address (case-insensitive via citext).
     * 
     * <p>Optional but unique when provided. PostgreSQL UNIQUE constraint allows multiple NULLs.
     */
    @Column(name = "email", columnDefinition = "citext")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * User's first name.
     */
    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * User's last name.
     */
    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * Contact phone number.
     */
    @Column(name = "phone_number", length = 50)
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    /**
     * Profile image URL.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Account enabled status.
     * 
     * <p>Used for soft delete and account suspension.
     */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // ========== Audit Fields ==========

    /**
     * Timestamp when account was created.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private OffsetDateTime createdDate;

    /**
     * Timestamp when account was last modified.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private OffsetDateTime lastModifiedDate;

    /**
     * User ID who created this account.
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    @Builder.Default
    private String createdBy = "system";

    /**
     * User ID who last modified this account.
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    /**
     * Pre-persist callback to ensure defaults.
     */
    @PrePersist
    protected void onCreate() {
        if (enabled == null) {
            enabled = true;
        }
        if (createdDate == null) {
            createdDate = OffsetDateTime.now();
        }
        if (createdBy == null) {
            createdBy = "system";
        }
    }

    /**
     * Pre-update callback.
     */
    @PreUpdate
    protected void onUpdate() {
        lastModifiedDate = OffsetDateTime.now();
    }
}
