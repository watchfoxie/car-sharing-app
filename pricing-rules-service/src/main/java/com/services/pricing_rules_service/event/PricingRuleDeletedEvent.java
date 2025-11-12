package com.services.pricing_rules_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a pricing rule is deleted.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PricingRuleDeletedEvent extends DomainEvent {

    /**
     * Pricing rule UUID that was deleted.
     */
    private String ruleId;

    /**
     * Car UUID (null if global rule).
     */
    private String carId;
}
