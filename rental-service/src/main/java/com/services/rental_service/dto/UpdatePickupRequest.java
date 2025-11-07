package com.services.rental_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for updating rental pickup information.
 * <p>
 * Used by PUT /v1/rentals/{id}/pickup endpoint.
 * Only allowed when rental status is CONFIRMED.
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
public class UpdatePickupRequest {

    /**
     * Actual pickup datetime.
     * Defaults to current timestamp if not provided.
     */
    private Instant actualPickupDatetime;

    /**
     * Actual pickup location (can differ from planned location).
     * Optional, defaults to planned pickupLocation if not provided.
     */
    private String actualPickupLocation;

    /**
     * Optional notes about vehicle condition at pickup.
     * Can include damage observations, fuel level, etc.
     */
    private String pickupNotes;
}
