package com.services.car_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a car is marked as shared (available for rental).
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Availability cache invalidation in car-service</li>
 *   <li>Potential notifications to nearby users</li>
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
public class CarSharedEvent extends DomainEvent {

    /**
     * Car UUID that was marked as shared.
     */
    private String carId;

    /**
     * Owner account ID.
     */
    private String ownerId;

    /**
     * Brand and model for logging/monitoring.
     */
    private String brand;
    private String model;
}
