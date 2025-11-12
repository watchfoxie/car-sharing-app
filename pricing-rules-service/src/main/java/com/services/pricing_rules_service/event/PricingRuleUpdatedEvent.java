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
 * Event published when an existing pricing rule is updated.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PricingRuleUpdatedEvent extends DomainEvent {

    /**
     * Pricing rule UUID.
     */
    private String ruleId;

    /**
     * Car UUID (null if global rule).
     */
    private String carId;

    /**
     * Updated start date.
     */
    private LocalDate startDate;

    /**
     * Updated end date.
     */
    private LocalDate endDate;

    /**
     * Updated price per day.
     */
    private BigDecimal pricePerDay;
}
