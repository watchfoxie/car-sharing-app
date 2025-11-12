package com.services.pricing_rules_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Event published when a new pricing rule is created.
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Pricing cache invalidation in rental-service</li>
 *   <li>Recalculation of affected pending rentals</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PricingRuleCreatedEvent extends DomainEvent {

    /**
     * Pricing rule UUID.
     */
    private String ruleId;

    /**
     * Car UUID (null if global rule).
     */
    private String carId;

    /**
     * Start date of the pricing rule.
     */
    private LocalDate startDate;

    /**
     * End date of the pricing rule.
     */
    private LocalDate endDate;

    /**
     * Price per day for this rule.
     */
    private BigDecimal pricePerDay;
}
