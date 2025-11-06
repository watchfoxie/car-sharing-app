package com.services.pricing_rules_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * DTO for price calculation responses.
 * 
 * <p>This response provides a detailed breakdown of the calculated rental cost, including:</p>
 * <ul>
 *   <li><strong>Total cost</strong> - Final price to charge the customer</li>
 *   <li><strong>Duration breakdown</strong> - How the total duration was split across pricing units (MINUTE/HOUR/DAY)</li>
 *   <li><strong>Metadata</strong> - Original request parameters and calculation timestamp</li>
 * </ul>
 * 
 * <p><strong>Cost Optimization:</strong></p>
 * <p>The pricing engine selects the most cost-effective combination of units. For example,
 * a 5.5-hour rental might be billed as:</p>
 * <ul>
 *   <li>5 HOUR units @ €12.00/hour = €60.00</li>
 *   <li>30 MINUTE units @ €0.30/minute = €9.00</li>
 *   <li><strong>Total: €69.00</strong></li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * CalculatePriceRequest request = CalculatePriceRequest.builder()
 *     .vehicleCategory(VehicleCategory.STANDARD)
 *     .pickupDatetime(Instant.parse("2025-01-10T10:00:00Z"))
 *     .returnDatetime(Instant.parse("2025-01-12T10:00:00Z")) // 2 days
 *     .build();
 * 
 * CalculatePriceResponse response = pricingRuleService.calculatePrice(request);
 * // Expected: 2 DAY units @ €80/day = €160.00
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.service.PricingRuleService#calculatePrice(CalculatePriceRequest)
 * @see CalculatePriceRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO containing calculated rental price with detailed breakdown by pricing units")
public class CalculatePriceResponse {

    /**
     * Total rental cost (sum of all unit costs).
     */
    @Schema(
        description = "Total rental cost in euros (sum of all unit costs)",
        example = "69.00"
    )
    private BigDecimal totalCost;

    /**
     * Total rental duration.
     */
    @Schema(
        description = "Total rental duration (ISO-8601 duration format)",
        example = "PT5H30M"
    )
    private Duration totalDuration;

    /**
     * Vehicle category used for calculation.
     */
    @Schema(
        description = "Vehicle category used for pricing calculation",
        example = "STANDARD"
    )
    private VehicleCategory vehicleCategory;

    /**
     * Original pickup datetime from request.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Rental start timestamp (ISO-8601 UTC format)",
        example = "2025-01-10T10:00:00Z"
    )
    private Instant pickupDatetime;

    /**
     * Original return datetime from request.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Rental end timestamp (ISO-8601 UTC format)",
        example = "2025-01-10T15:30:00Z"
    )
    private Instant returnDatetime;

    /**
     * Breakdown of cost by pricing unit.
     * 
     * <p>Each entry shows how many units of a specific type (MINUTE/HOUR/DAY) were charged
     * and at what rate.</p>
     */
    @Schema(
        description = "Detailed breakdown of cost by pricing unit (MINUTE, HOUR, DAY)",
        example = "[{\"unit\": \"HOUR\", \"quantity\": 5, \"pricePerUnit\": 12.00, \"subtotal\": 60.00}, {\"unit\": \"MINUTE\", \"quantity\": 30, \"pricePerUnit\": 0.30, \"subtotal\": 9.00}]"
    )
    private List<UnitBreakdown> breakdown;

    /**
     * Timestamp when the calculation was performed.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(
        description = "Timestamp when the calculation was performed (ISO-8601 UTC format)",
        example = "2025-01-09T14:22:00Z"
    )
    private Instant calculatedAt;

    /**
     * Nested DTO representing cost breakdown for a single pricing unit.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Breakdown of cost for a single pricing unit")
    public static class UnitBreakdown {

        /**
         * Pricing unit (MINUTE, HOUR, or DAY).
         */
        @Schema(description = "Pricing unit type", example = "HOUR")
        private PricingUnit unit;

        /**
         * Number of units charged.
         */
        @Schema(description = "Quantity of units charged", example = "5")
        private long quantity;

        /**
         * Price per single unit.
         */
        @Schema(description = "Price per single unit", example = "12.00")
        private BigDecimal pricePerUnit;

        /**
         * Subtotal for this unit type (quantity * pricePerUnit).
         */
        @Schema(description = "Subtotal for this unit type (quantity × pricePerUnit)", example = "60.00")
        private BigDecimal subtotal;
    }
}
