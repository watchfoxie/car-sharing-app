package com.services.car_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event published when a new car is created in the system.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CarCreatedEvent extends DomainEvent {

    /**
     * Car UUID.
     */
    private String carId;

    /**
     * Owner account ID.
     */
    private String ownerId;

    /**
     * Brand of the car (e.g., "Toyota", "BMW").
     */
    private String brand;

    /**
     * Model name (e.g., "Corolla", "X5").
     */
    private String model;

    /**
     * Whether the car is shareable.
     */
    private boolean shareable;

    /**
     * Base price per day.
     */
    private BigDecimal basePrice;
}
