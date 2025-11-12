package com.services.rental_service.outbox;

import com.services.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Outbox entity for rental-service domain events.
 * Maps to the rental.outbox_event table.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Entity
@Table(name = "outbox_event", schema = "rental")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RentalOutboxEvent extends OutboxEvent {
}
