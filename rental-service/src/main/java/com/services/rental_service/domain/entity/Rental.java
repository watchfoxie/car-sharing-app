package com.services.rental_service.domain.entity;

import com.services.rental_service.domain.enums.RentalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a car rental in the Car Sharing platform.
 * <p>
 * Mapped to the {@code rental.cars_rental_history} table in PostgreSQL.
 * Implements a Finite State Machine (FSM) for rental lifecycle management.
 * </p>
 * <p>
 * <strong>Key constraints:</strong>
 * <ul>
 *   <li>EXCLUDE constraint on {@code (cars_id, rental_period)} prevents overlapping
 *       active rentals (CONFIRMED, PICKED_UP states only)</li>
 *   <li>Unique constraint on {@code (renter_id, idempotency_key)} ensures idempotent
 *       rental creation</li>
 *   <li>CHECK constraint: {@code return_datetime >= pickup_datetime}</li>
 *   <li>CHECK constraint: RETURNED/RETURN_APPROVED states require return_datetime</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Generated column:</strong> {@code rental_period} TSTZRANGE is auto-computed
 * by PostgreSQL as {@code [pickup_datetime, return_datetime)} with upper bound defaulting
 * to 100 years for open-ended rentals.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Entity
@Table(name = "cars_rental_history", schema = "rental")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rental {

    /**
     * Primary key, auto-generated sequence.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the renter (customer) from identity.accounts.
     * Foreign key with ON DELETE RESTRICT.
     */
    @NotBlank(message = "Renter ID is required")
    @Column(name = "renter_id", nullable = false)
    private String renterId;

    /**
     * ID of the car from car.cars.
     * Foreign key with ON DELETE CASCADE.
     */
    @NotNull(message = "Car ID is required")
    @Column(name = "cars_id", nullable = false)
    private Long carsId;

    /**
     * Pickup datetime (start of rental period).
     * Must be in the future at creation time.
     */
    @NotNull(message = "Pickup datetime is required")
    @Column(name = "pickup_datetime", nullable = false)
    private Instant pickupDatetime;

    /**
     * Return datetime (end of rental period).
     * NULL for open-ended rentals, set when customer returns vehicle.
     * Must be >= pickup_datetime (DB CHECK constraint).
     */
    @Column(name = "return_datetime")
    private Instant returnDatetime;

    /**
     * Pickup location (address or coordinates).
     */
    @Column(name = "pickup_location")
    private String pickupLocation;

    /**
     * Return location (address or coordinates).
     */
    @Column(name = "return_location")
    private String returnLocation;

    /**
     * Current status in the rental lifecycle FSM.
     * Default: PENDING
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "rental.rental_status")
    private RentalStatus status;

    /**
     * Estimated cost at booking time (from pricing-rules-service).
     * May differ from final_cost if rental is extended or late.
     */
    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    /**
     * Final cost after return approval, including penalties.
     * Calculated during return-approval workflow.
     */
    @Column(name = "final_cost", precision = 10, scale = 2)
    private BigDecimal finalCost;

    /**
     * Idempotency key for duplicate request prevention.
     * Unique per (renter_id, idempotency_key).
     * Format: UUID or custom client-generated key.
     */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    /**
     * Timestamp when the rental record was created.
     * Populated automatically by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * Timestamp of the last modification.
     * Updated automatically by JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    /**
     * User ID who created the record (from JWT sub claim).
     * Populated automatically by JPA auditing.
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    /**
     * User ID who last modified the record (from JWT sub claim).
     * Updated automatically by JPA auditing.
     */
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    /**
     * Business logic: Check if rental can be cancelled.
     * Only PENDING and CONFIRMED states allow cancellation.
     *
     * @return true if cancellation is allowed, false otherwise
     */
    public boolean isCancellable() {
        return status == RentalStatus.PENDING || status == RentalStatus.CONFIRMED;
    }

    /**
     * Business logic: Check if rental can be picked up.
     * Only CONFIRMED state allows pickup.
     *
     * @return true if pickup is allowed, false otherwise
     */
    public boolean canPickup() {
        return status == RentalStatus.CONFIRMED;
    }

    /**
     * Business logic: Check if rental can be returned.
     * Only PICKED_UP state allows return.
     *
     * @return true if return is allowed, false otherwise
     */
    public boolean canReturn() {
        return status == RentalStatus.PICKED_UP;
    }

    /**
     * Business logic: Check if return can be approved.
     * Only RETURNED state allows approval.
     *
     * @return true if return approval is allowed, false otherwise
     */
    public boolean canApproveReturn() {
        return status == RentalStatus.RETURNED;
    }

    /**
     * Business logic: Check if rental is in an active state (blocks car availability).
     * CONFIRMED and PICKED_UP states are considered active.
     *
     * @return true if rental blocks car availability, false otherwise
     */
    public boolean isActive() {
        return status == RentalStatus.CONFIRMED || status == RentalStatus.PICKED_UP;
    }

    /**
     * Business logic: Check if rental is owned by the specified renter.
     *
     * @param accountId the account ID to check
     * @return true if accountId matches renterId, false otherwise
     */
    public boolean isOwnedBy(String accountId) {
        return renterId != null && renterId.equals(accountId);
    }
}
