package com.services.pricing_rules_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * DTO for updating an existing pricing rule.
 * 
 * <p>All fields are optional to support partial updates (PATCH semantics).
 * Only non-null fields will be applied to the target entity via MapStruct.</p>
 * 
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>If {@code pricePerUnit} is provided, it must be >= 0.00</li>
 *   <li>If {@code lateReturnPenaltyPercent} is provided, it must be between 0.00 and 100.00</li>
 *   <li>Cross-field validations (effectiveTo > effectiveFrom, maxDuration >= minDuration) are handled in service layer</li>
 * </ul>
 * 
 * <p><strong>Immutable Fields:</strong></p>
 * <ul>
 *   <li>{@code id} - Cannot be changed (URL path parameter)</li>
 *   <li>Audit fields (createdDate, createdBy) - Managed by JPA Auditing</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Update only price and penalty
 * UpdatePricingRuleRequest request = UpdatePricingRuleRequest.builder()
 *     .pricePerUnit(new BigDecimal("15.00"))
 *     .lateReturnPenaltyPercent(new BigDecimal("30.00"))
 *     .build();
 * 
 * PricingRuleResponse response = pricingRuleService.updateRule(ruleId, request);
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.service.PricingRuleService#updateRule(Long, UpdatePricingRuleRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for updating an existing pricing rule (all fields optional for partial updates)")
public class UpdatePricingRuleRequest {

    /**
     * Time unit for pricing (MINUTE, HOUR, DAY).
     * 
     * <p><strong>Note:</strong> Changing the unit may cause EXCLUDE constraint violations if overlapping rules exist.</p>
     */
    @Schema(
        description = "Time unit for pricing calculation",
        example = "HOUR"
    )
    private PricingUnit unit;

    /**
     * Vehicle category (ECONOM, STANDARD, PREMIUM).
     * 
     * <p><strong>Note:</strong> Changing the category may cause EXCLUDE constraint violations if overlapping rules exist.</p>
     */
    @Schema(
        description = "Vehicle category for which the rule applies",
        example = "STANDARD"
    )
    private VehicleCategory vehicleCategory;

    /**
     * Price per unit of time.
     */
    @DecimalMin(value = "0.00", inclusive = true, message = "Price per unit must be >= 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    @Schema(
        description = "Price per unit of time (e.g., €15.00 per HOUR)",
        example = "15.00",
        minimum = "0.00"
    )
    private BigDecimal pricePerUnit;

    /**
     * Minimum rental duration (optional).
     */
    @Schema(
        description = "Minimum rental duration (ISO-8601 duration format)",
        example = "PT1H"
    )
    private Duration minDuration;

    /**
     * Maximum rental duration (optional).
     */
    @Schema(
        description = "Maximum rental duration (ISO-8601 duration format)",
        example = "P7D"
    )
    private Duration maxDuration;

    /**
     * Cancellation window before pickup (optional).
     */
    @Schema(
        description = "Free cancellation window before pickup (ISO-8601 duration format)",
        example = "PT2H"
    )
    private Duration cancellationWindow;

    /**
     * Late return penalty as percentage (0-100%).
     */
    @DecimalMin(value = "0.00", inclusive = true, message = "Late penalty must be >= 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "Late penalty must be <= 100")
    @Digits(integer = 3, fraction = 2, message = "Penalty must have at most 3 integer digits and 2 decimal places")
    @Schema(
        description = "Late return penalty as percentage of base cost (0-100%)",
        example = "30.00",
        minimum = "0.00",
        maximum = "100.00"
    )
    private BigDecimal lateReturnPenaltyPercent;

    /**
     * Start of validity period.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Start of rule validity (ISO-8601 UTC timestamp)",
        example = "2025-01-01T00:00:00Z"
    )
    private Instant effectiveFrom;

    /**
     * End of validity period (optional, NULL means indefinite).
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "End of rule validity (ISO-8601 UTC timestamp). NULL means indefinite.",
        example = "2025-12-31T23:59:59Z"
    )
    private Instant effectiveTo;

    /**
     * Whether the rule is active.
     */
    @Schema(
        description = "Whether the rule is active and should be used for calculations",
        example = "true"
    )
    private Boolean active;
}
