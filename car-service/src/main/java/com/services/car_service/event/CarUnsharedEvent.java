package com.services.car_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a car is marked as unshared (not available for rental).
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Availability cache invalidation</li>
 *   <li>Cancellation of pending rental requests</li>
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
public class CarUnsharedEvent extends DomainEvent {

    /**
     * Car UUID that was marked as unshared.
     */
    private String carId;

    /**
     * Owner account ID.
     */
    private String ownerId;
}
