package com.services.car_service.outbox;

import com.services.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Outbox entity for car-service domain events.
 * Maps to the car.outbox_event table.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Entity
@Table(name = "outbox_event", schema = "car")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CarOutboxEvent extends OutboxEvent {
}
