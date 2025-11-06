package com.services.pricing_rules_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * DTO for pricing rule responses (read-only).
 * 
 * <p>This DTO represents the complete state of a pricing rule as returned by API endpoints.
 * It includes all business fields plus audit metadata for transparency and debugging.</p>
 * 
 * <p><strong>Usage Scenarios:</strong></p>
 * <ul>
 *   <li>GET /v1/pricing/rules/{id} - Single rule retrieval</li>
 *   <li>GET /v1/pricing/rules - List of rules (paginated)</li>
 *   <li>POST /v1/pricing/rules - Response after creation</li>
 *   <li>PUT /v1/pricing/rules/{id} - Response after update</li>
 * </ul>
 * 
 * <p><strong>Audit Fields:</strong></p>
 * <ul>
 *   <li>{@code createdDate}, {@code createdBy} - Who and when created the rule</li>
 *   <li>{@code lastModifiedDate}, {@code lastModifiedBy} - Who and when last modified</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.service.PricingRuleService
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response DTO representing a pricing rule with full details and audit metadata")
public class PricingRuleResponse {

    @Schema(description = "Unique identifier of the pricing rule", example = "1")
    private Long id;

    @Schema(description = "Time unit for pricing calculation", example = "HOUR")
    private PricingUnit unit;

    @Schema(description = "Vehicle category for which the rule applies", example = "STANDARD")
    private VehicleCategory vehicleCategory;

    @Schema(description = "Price per unit of time", example = "12.00")
    private BigDecimal pricePerUnit;

    @Schema(description = "Minimum rental duration (ISO-8601 duration format)", example = "PT1H")
    private Duration minDuration;

    @Schema(description = "Maximum rental duration (ISO-8601 duration format)", example = "P7D")
    private Duration maxDuration;

    @Schema(description = "Free cancellation window before pickup (ISO-8601 duration format)", example = "PT2H")
    private Duration cancellationWindow;

    @Schema(description = "Late return penalty as percentage of base cost (0-100%)", example = "25.00")
    private BigDecimal lateReturnPenaltyPercent;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(description = "Start of rule validity (ISO-8601 UTC timestamp)", example = "2025-01-01T00:00:00Z")
    private Instant effectiveFrom;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(description = "End of rule validity (ISO-8601 UTC timestamp). NULL means indefinite.", example = "2025-12-31T23:59:59Z")
    private Instant effectiveTo;

    @Schema(description = "Whether the rule is active and should be used for calculations", example = "true")
    private Boolean active;

    // Audit fields

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(description = "Timestamp when the rule was created", example = "2025-01-01T10:30:00Z")
    private Instant createdDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @Schema(description = "Timestamp when the rule was last modified", example = "2025-01-15T14:45:00Z")
    private Instant lastModifiedDate;

    @Schema(description = "Account ID of the user who created the rule", example = "admin-001")
    private String createdBy;

    @Schema(description = "Account ID of the user who last modified the rule", example = "operator-042")
    private String lastModifiedBy;
}
