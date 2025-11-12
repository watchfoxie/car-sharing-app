package com.services.car_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a car is deleted from the system.
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Cache invalidation for the car</li>
 *   <li>Cleanup of associated rentals (if any)</li>
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
public class CarDeletedEvent extends DomainEvent {

    /**
     * Car UUID that was deleted.
     */
    private String carId;

    /**
     * Owner account ID.
     */
    private String ownerId;
}
