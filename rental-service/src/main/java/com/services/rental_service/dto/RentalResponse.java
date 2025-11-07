package com.services.rental_service.dto;

import com.services.rental_service.domain.enums.RentalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for rental responses.
 * <p>
 * Read-only representation of a rental, returned by GET endpoints.
 * Includes calculated fields and audit information.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalResponse {

    /**
     * Unique rental ID.
     */
    private Long id;

    /**
     * ID of the renter (customer).
     */
    private String renterId;

    /**
     * ID of the rented car.
     */
    private Long carsId;

    /**
     * Pickup datetime (start of rental period).
     */
    private Instant pickupDatetime;

    /**
     * Return datetime (end of rental period).
     * NULL if not yet returned.
     */
    private Instant returnDatetime;

    /**
     * Pickup location.
     */
    private String pickupLocation;

    /**
     * Return location.
     */
    private String returnLocation;

    /**
     * Current rental status.
     */
    private RentalStatus status;

    /**
     * Estimated cost at booking time.
     */
    private BigDecimal estimatedCost;

    /**
     * Final cost after return approval (includes penalties).
     * NULL until RETURN_APPROVED.
     */
    private BigDecimal finalCost;

    /**
     * Idempotency key used at creation.
     */
    private String idempotencyKey;

    /**
     * Creation timestamp.
     */
    private Instant createdDate;

    /**
     * Last modification timestamp.
     */
    private Instant lastModifiedDate;

    /**
     * User who created the rental.
     */
    private String createdBy;

    /**
     * User who last modified the rental.
     */
    private String lastModifiedBy;
}
