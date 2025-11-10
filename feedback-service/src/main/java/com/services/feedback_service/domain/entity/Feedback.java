package com.services.feedback_service.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * JPA Entity representing customer feedback for a car.
 * 
 * <p>Maps to {@code feedback.cars_feedback} table in PostgreSQL.
 * 
 * <p>Business rules:
 * <ul>
 *   <li>Rating must be between 0 and 5 (inclusive)</li>
 *   <li>Comment is optional but recommended</li>
 *   <li>Each renter can submit feedback once per completed rental</li>
 *   <li>Feedback is linked to a specific car (cars_id)</li>
 *   <li>If reviewer account is deleted, feedback remains but becomes anonymous (reviewer_id = NULL)</li>
 * </ul>
 * 
 * <p>Audit trail:
 * <ul>
 *   <li>{@code created_date}: Auto-populated on insert</li>
 *   <li>{@code created_by}: Extracted from JWT sub claim</li>
 *   <li>{@code last_modified_date}: Auto-updated on update</li>
 *   <li>{@code last_modified_by}: Extracted from JWT sub claim</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Entity
@Table(name = "cars_feedback", schema = "feedback")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    /**
     * Primary key (auto-generated sequence).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Rating value (0.0 to 5.0).
     * 
     * <p>Constraint: {@code CHECK (rating BETWEEN 0 AND 5)}
     */
    @Column(nullable = false)
    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5")
    private Double rating;

    /**
     * Optional comment from the reviewer.
     * 
     * <p>Max length: unlimited (TEXT column)
     */
    @Column(columnDefinition = "TEXT")
    @Size(max = 5000, message = "Comment must not exceed 5000 characters")
    private String comment;

    /**
     * Foreign key to {@code car.cars.id}.
     * 
     * <p>ON DELETE CASCADE: Feedback is deleted if car is deleted.
     */
    @Column(name = "cars_id", nullable = false)
    @NotNull(message = "Car ID is required")
    private Long carsId;

    /**
     * Foreign key to {@code identity.accounts.id} (reviewer).
     * 
     * <p>ON DELETE SET NULL: If reviewer account is deleted, feedback remains but becomes anonymous.
     */
    @Column(name = "reviewer_id")
    private String reviewerId;

    /**
     * Timestamp when feedback was created (UTC).
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * Timestamp when feedback was last modified (UTC).
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    /**
     * User ID who created this feedback (from JWT sub claim).
     * 
     * <p>Auto-populated by JPA Auditing (via {@link JpaAuditingConfig}).
     * In production: set to JWT sub claim. In tests: set to "system".
     * 
     * <p>Note: nullable=false in production (enforced by Flyway migration),
     * but tests use Hibernate DDL which requires nullable=true for JPA Auditing.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    /**
     * User ID who last modified this feedback (from JWT sub claim).
     * 
     * <p>Auto-populated by JPA Auditing (via {@link JpaAuditingConfig}).
     * In production: set to JWT sub claim. In tests: set to "system".
     * 
     * <p>Note: nullable column for flexibility (audit metadata, not business data).
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    /**
     * Business method to check if feedback belongs to a specific reviewer.
     * 
     * @param accountId the account ID to check
     * @return {@code true} if this feedback was submitted by the given account
     */
    public boolean isReviewedBy(String accountId) {
        return reviewerId != null && reviewerId.equals(accountId);
    }

    /**
     * Business method to check if feedback is anonymous (reviewer account deleted).
     * 
     * @return {@code true} if reviewer_id is NULL
     */
    public boolean isAnonymous() {
        return reviewerId == null;
    }
}
