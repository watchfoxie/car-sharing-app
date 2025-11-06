package com.services.pricing_rules_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * DTO for price calculation requests.
 * 
 * <p>This request contains the minimum information needed to calculate rental cost:</p>
 * <ul>
 *   <li><strong>vehicleCategory</strong> - Determines which pricing rules to apply (ECONOM, STANDARD, PREMIUM)</li>
 *   <li><strong>pickupDatetime</strong> - Start of rental period (inclusive)</li>
 *   <li><strong>returnDatetime</strong> - End of rental period (exclusive)</li>
 * </ul>
 * 
 * <p><strong>Calculation Logic:</strong></p>
 * <ol>
 *   <li>Calculate total duration: {@code returnDatetime - pickupDatetime}</li>
 *   <li>Fetch active pricing rules for {@code vehicleCategory} at {@code pickupDatetime}</li>
 *   <li>Break down duration into MINUTE/HOUR/DAY units for cost optimization</li>
 *   <li>Apply min/max duration validations and late penalties if applicable</li>
 *   <li>Return total cost with breakdown by unit</li>
 * </ol>
 * 
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>All fields are required</li>
 *   <li>{@code returnDatetime} must be after {@code pickupDatetime} (validated in service layer)</li>
 *   <li>Duration must fall within configured min/max bounds for the pricing rule</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * CalculatePriceRequest request = CalculatePriceRequest.builder()
 *     .vehicleCategory(VehicleCategory.STANDARD)
 *     .pickupDatetime(Instant.parse("2025-01-10T10:00:00Z"))
 *     .returnDatetime(Instant.parse("2025-01-10T15:30:00Z")) // 5.5 hours
 *     .build();
 * 
 * CalculatePriceResponse response = pricingRuleService.calculatePrice(request);
 * // Expected: 5 hours @ €12/hour + 30 minutes @ €0.30/min = €60 + €9 = €69
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.service.PricingRuleService#calculatePrice(CalculatePriceRequest)
 * @see CalculatePriceResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for calculating rental price based on vehicle category and rental period")
public class CalculatePriceRequest {

    /**
     * Vehicle category for which to calculate price.
     * 
     * <p>This determines which set of pricing rules (MINUTE/HOUR/DAY) will be applied.</p>
     */
    @NotNull(message = "Vehicle category is required")
    @Schema(
        description = "Vehicle category (determines applicable pricing rules)",
        example = "STANDARD",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private VehicleCategory vehicleCategory;

    /**
     * Rental start timestamp (inclusive).
     * 
     * <p><strong>Timezone:</strong> Must be provided in UTC (ISO-8601 format).
     * Example: {@code 2025-01-10T10:00:00Z}</p>
     */
    @NotNull(message = "Pickup datetime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Rental start timestamp (ISO-8601 UTC format, inclusive)",
        example = "2025-01-10T10:00:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant pickupDatetime;

    /**
     * Rental end timestamp (exclusive).
     * 
     * <p><strong>Timezone:</strong> Must be provided in UTC (ISO-8601 format).
     * Example: {@code 2025-01-10T15:30:00Z}</p>
     * 
     * <p><strong>Validation:</strong> Must be after {@code pickupDatetime}.
     * Service layer will throw {@code ValidationException} if this constraint is violated.</p>
     */
    @NotNull(message = "Return datetime is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Rental end timestamp (ISO-8601 UTC format, exclusive)",
        example = "2025-01-10T15:30:00Z",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Instant returnDatetime;
}
