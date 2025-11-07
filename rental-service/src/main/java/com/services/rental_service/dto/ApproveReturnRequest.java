package com.services.rental_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for return approval by operator.
 * <p>
 * Used by PUT /v1/rentals/{id}/return-approval endpoint.
 * Only allowed when rental status is RETURNED.
 * Only the car owner (operator) can approve returns.
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
public class ApproveReturnRequest {

    /**
     * Operator's notes about vehicle condition after inspection.
     * Can include damage assessment, cleaning requirements, etc.
     */
    private String operatorNotes;

    /**
     * Additional charges beyond base rental cost.
     * Examples: damage fees, cleaning fees, late return penalties.
     * If NULL, calculated automatically by pricing-rules-service.
     */
    private BigDecimal additionalCharges;

    /**
     * Indicates if vehicle inspection passed without issues.
     * If false, additionalCharges should be provided.
     */
    private Boolean inspectionPassed;
}
