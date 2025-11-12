package com.services.pricing_rules_service.outbox;

import com.services.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Outbox entity for pricing-rules-service domain events.
 * Maps to the pricing.outbox_event table.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Entity
@Table(name = "outbox_event", schema = "pricing")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class PricingOutboxEvent extends OutboxEvent {
}
