package com.services.pricing_rules_service.domain.entity;

import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * JPA Entity representing a pricing rule for vehicle rentals.
 * 
 * <p>Pricing rules define the cost structure for renting vehicles based on:</p>
 * <ul>
 *   <li><strong>Time unit</strong>: MINUTE, HOUR, or DAY</li>
 *   <li><strong>Vehicle category</strong>: ECONOM, STANDARD, or PREMIUM</li>
 *   <li><strong>Temporal validity</strong>: Rules are effective within [effective_from, effective_to)</li>
 *   <li><strong>Operational policies</strong>: Min/max duration, cancellation windows, late penalties</li>
 * </ul>
 * 
 * <p><strong>Database Constraints:</strong></p>
 * <ul>
 *   <li><strong>EXCLUDE constraint</strong>: Prevents overlapping rules for the same (vehicle_category, unit) during the same time period</li>
 *   <li><strong>Check constraint</strong>: Ensures effective_to > effective_from when effective_to is set</li>
 *   <li><strong>Check constraint</strong>: Ensures max_duration >= min_duration when both are set</li>
 *   <li><strong>Generated column</strong>: effective_period (tstzrange) is auto-computed from effective_from/effective_to</li>
 * </ul>
 * 
 * <p><strong>Audit Trail:</strong></p>
 * <ul>
 *   <li>Audit fields (created_date, created_by, last_modified_date, last_modified_by) are auto-populated via JPA Auditing</li>
 *   <li>Trigger function {@code public.set_audit_fields()} provides additional database-level auditing</li>
 * </ul>
 * 
 * <p><strong>Caching Strategy:</strong></p>
 * <ul>
 *   <li>Active rules are cached in Redis (TTL: 10 minutes) for inter-service sharing</li>
 *   <li>Frequently accessed rules are cached locally in Caffeine (TTL: 5 minutes, max 500 entries)</li>
 *   <li>Cache invalidation occurs on create/update/delete operations</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Creating a pricing rule for STANDARD category, hourly rate €12.00
 * PricingRule rule = PricingRule.builder()
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
 * pricingRuleRepository.save(rule);
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Entity
@Table(name = "pricing_rule", schema = "pricing")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PricingRule {

    /**
     * Primary key (auto-generated sequence).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Time unit for pricing (MINUTE, HOUR, DAY).
     * 
     * <p>Combined with {@code vehicleCategory}, this forms a unique constraint
     * during the rule's effective period (enforced by EXCLUDE constraint).</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Pricing unit is required")
    private PricingUnit unit;

    /**
     * Vehicle category (ECONOM, STANDARD, PREMIUM).
     * 
     * <p>Synchronized with {@code car.cars.category} enum.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_category", nullable = false, length = 20)
    @NotNull(message = "Vehicle category is required")
    private VehicleCategory vehicleCategory;

    /**
     * Price per unit of time (e.g., €12.00 per HOUR).
     * 
     * <p>Must be >= 0. Precision: 10 digits, scale: 2 (e.g., 99999999.99).</p>
     */
    @Column(name = "price_per_unit", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price per unit is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "Price per unit must be >= 0")
    private BigDecimal pricePerUnit;

    /**
     * Minimum rental duration (nullable).
     * 
     * <p>If set, rentals shorter than this duration will be rejected or rounded up.
     * Example: {@code Duration.ofHours(1)} means rentals must be at least 1 hour.</p>
     */
    @Column(name = "min_duration")
    private Duration minDuration;

    /**
     * Maximum rental duration (nullable).
     * 
     * <p>If set, rentals longer than this duration will be rejected or require special approval.
     * Example: {@code Duration.ofDays(7)} means rentals cannot exceed 7 days.</p>
     */
    @Column(name = "max_duration")
    private Duration maxDuration;

    /**
     * Cancellation window before pickup (nullable).
     * 
     * <p>If set, cancellations within this window may incur penalties.
     * Example: {@code Duration.ofHours(2)} means free cancellations only if done 2+ hours before pickup.</p>
     */
    @Column(name = "cancellation_window")
    private Duration cancellationWindow;

    /**
     * Late return penalty as percentage of base cost (nullable, 0-100%).
     * 
     * <p>Applied when vehicle is returned after the scheduled return time.
     * Example: {@code 25.00} means 25% surcharge on the base rental cost.</p>
     */
    @Column(name = "late_return_penalty_percent", precision = 5, scale = 2)
    @DecimalMin(value = "0.00", inclusive = true, message = "Late penalty must be >= 0")
    @DecimalMax(value = "100.00", inclusive = true, message = "Late penalty must be <= 100")
    private BigDecimal lateReturnPenaltyPercent;

    /**
     * Start of the rule's validity period (inclusive).
     * 
     * <p>Rules are applicable for rentals starting on or after this timestamp.</p>
     */
    @Column(name = "effective_from", nullable = false)
    @NotNull(message = "Effective from date is required")
    private Instant effectiveFrom;

    /**
     * End of the rule's validity period (exclusive, nullable).
     * 
     * <p>If NULL, the rule is valid indefinitely (until explicitly deactivated).
     * Rules are applicable for rentals starting before this timestamp.</p>
     */
    @Column(name = "effective_to")
    private Instant effectiveTo;

    /**
     * Computed tstzrange column for temporal constraint enforcement.
     * 
     * <p><strong>Note:</strong> This field is auto-generated by PostgreSQL and should NOT be set manually.
     * The EXCLUDE constraint uses this column to prevent overlapping rules.</p>
     * 
     * <p><strong>Formula:</strong></p>
     * <pre>{@code
     * GENERATED ALWAYS AS (
     *   tstzrange(effective_from, COALESCE(effective_to, 'infinity'::timestamptz), '[)')
     * ) STORED
     * }</pre>
     */
    @Column(name = "effective_period", insertable = false, updatable = false)
    private String effectivePeriod; // Represented as String in JPA (PostgreSQL tstzrange type)

    /**
     * Indicates if the rule is currently active.
     * 
     * <p>Inactive rules are not used for calculations. This flag allows soft-disabling
     * of rules without deletion (e.g., for regulatory compliance or historical tracking).</p>
     */
    @Column(nullable = false)
    @NotNull
    @Builder.Default
    private Boolean active = true;

    // ==================== Audit Fields ====================

    /**
     * Timestamp when the rule was created (auto-populated).
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * Timestamp when the rule was last modified (auto-updated).
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    /**
     * Account ID of the user who created the rule (auto-populated from JWT sub claim).
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    /**
     * Account ID of the user who last modified the rule (auto-updated from JWT sub claim).
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    // ==================== Business Methods ====================

    /**
     * Checks if the rule is currently effective (active and within validity period).
     * 
     * <p>A rule is effective if:</p>
     * <ul>
     *   <li>{@code active == true}</li>
     *   <li>{@code now >= effectiveFrom}</li>
     *   <li>{@code now < effectiveTo} (or effectiveTo is NULL)</li>
     * </ul>
     * 
     * @param now The current timestamp to check against
     * @return {@code true} if the rule is effective, {@code false} otherwise
     */
    public boolean isEffectiveAt(Instant now) {
        if (!active) return false;
        if (now.isBefore(effectiveFrom)) return false;
        return effectiveTo == null || now.isBefore(effectiveTo);
    }

    /**
     * Validates if a rental duration is within the configured min/max bounds.
     * 
     * @param duration The rental duration to validate
     * @return {@code true} if valid, {@code false} otherwise
     */
    public boolean isValidDuration(Duration duration) {
        if (minDuration != null && duration.compareTo(minDuration) < 0) {
            return false;
        }
        if (maxDuration != null && duration.compareTo(maxDuration) > 0) {
            return false;
        }
        return true;
    }

    /**
     * Calculates if a cancellation at the given time would incur penalties.
     * 
     * @param cancellationTime The time when cancellation is requested
     * @param pickupTime The scheduled pickup time
     * @return {@code true} if within free cancellation window, {@code false} if penalties apply
     */
    public boolean isWithinCancellationWindow(Instant cancellationTime, Instant pickupTime) {
        if (cancellationWindow == null) {
            return true; // No cancellation restrictions
        }
        Duration timeUntilPickup = Duration.between(cancellationTime, pickupTime);
        return timeUntilPickup.compareTo(cancellationWindow) >= 0;
    }
}
