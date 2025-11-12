package com.services.rental_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when the owner approves the car return (FSM: COMPLETED state).
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Car becomes available again in car-service</li>
 *   <li>Feedback collection workflow starts</li>
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
public class RentalReturnApprovedEvent extends DomainEvent {

    /**
     * Rental UUID.
     */
    private String rentalId;

    /**
     * Car UUID now available again.
     */
    private String carId;
}
