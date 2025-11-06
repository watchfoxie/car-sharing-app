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
 * DTO for creating a new pricing rule.
 * 
 * <p>This request DTO contains all required and optional fields for defining a pricing rule.
 * It enforces validation constraints to ensure data integrity before persistence.</p>
 * 
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>{@code unit}, {@code vehicleCategory}, {@code pricePerUnit}, {@code effectiveFrom} are required</li>
 *   <li>{@code pricePerUnit} must be >= 0.00</li>
 *   <li>{@code lateReturnPenaltyPercent} must be between 0.00 and 100.00 (if provided)</li>
 *   <li>{@code effectiveTo} must be after {@code effectiveFrom} (validated in service layer)</li>
 *   <li>{@code maxDuration} must be >= {@code minDuration} (if both provided, validated in service layer)</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * CreatePricingRuleRequest request = CreatePricingRuleRequest.builder()
 *     .unit(PricingUnit.HOUR)
 *     .vehicleCategory(VehicleCategory.STANDARD)
 *     .pricePerUnit(new BigDecimal("12.00"))
 *     .minDuration(Duration.ofHours(1))
 *     .maxDuration(Duration.ofHours(24))
 *     .cancellationWindow(Duration.ofHours(2))
 *     .lateReturnPenaltyPercent(new BigDecimal("25.00"))
 *     .effectiveFrom(Instant.now())
 *     .active(true)
 *     .build();
 * 
 * PricingRuleResponse response = pricingRuleService.createRule(request);
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.service.PricingRuleService#createRule(CreatePricingRuleRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for creating a new pricing rule")
public class CreatePricingRuleRequest {

    /**
     * Time unit for pricing (MINUTE, HOUR, DAY).
     */
    @NotNull(message = "Pricing unit is required")
    @Schema(
        description = "Time unit for pricing calculation",
        example = "HOUR",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private PricingUnit unit;

    /**
     * Vehicle category (ECONOM, STANDARD, PREMIUM).
     */
    @NotNull(message = "Vehicle category is required")
    @Schema(
        description = "Vehicle category for which the rule applies",
        example = "STANDARD",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private VehicleCategory vehicleCategory;

    /**
     * Price per unit of time.
     */
    @NotNull(message = "Price per unit is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price per unit must be >= 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    @Schema(
        description = "Price per unit of time (e.g., €12.00 per HOUR)",
        example = "12.00",
        minimum = "0.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private BigDecimal pricePerUnit;

    /**
     * Minimum rental duration (optional).
     */
    @Schema(
        description = "Minimum rental duration (ISO-8601 duration format: PT1H for 1 hour, PT30M for 30 minutes)",
        example = "PT1H"
    )
    private Duration minDuration;

    /**
     * Maximum rental duration (optional).
     */
    @Schema(
        description = "Maximum rental duration (ISO-8601 duration format: P7D for 7 days)",
        example = "P7D"
    )
    private Duration maxDuration;

    /**
     * Cancellation window before pickup (optional).
     */
    @Schema(
        description = "Free cancellation window before pickup (ISO-8601 duration format: PT2H for 2 hours)",
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
        example = "25.00",
        minimum = "0.00",
        maximum = "100.00"
    )
    private BigDecimal lateReturnPenaltyPercent;

    /**
     * Start of validity period.
     */
    @NotNull(message = "Effective from date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Start of rule validity (ISO-8601 UTC timestamp)",
        example = "2025-01-01T00:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
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
    @NotNull(message = "Active flag is required")
    @Schema(
        description = "Whether the rule is active and should be used for calculations",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED,
        defaultValue = "true"
    )
    @Builder.Default
    private Boolean active = true;
}
