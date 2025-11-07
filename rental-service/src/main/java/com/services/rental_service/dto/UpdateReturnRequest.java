package com.services.rental_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for updating rental return information.
 * <p>
 * Used by PUT /v1/rentals/{id}/return endpoint.
 * Only allowed when rental status is PICKED_UP.
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
public class UpdateReturnRequest {

    /**
     * Actual return datetime.
     * Defaults to current timestamp if not provided.
     */
    private Instant actualReturnDatetime;

    /**
     * Actual return location (can differ from planned location).
     * Optional, defaults to planned returnLocation if not provided.
     */
    private String actualReturnLocation;

    /**
     * Optional notes about vehicle condition at return.
     * Can include damage observations, fuel level, cleanliness, etc.
     */
    private String returnNotes;
}
