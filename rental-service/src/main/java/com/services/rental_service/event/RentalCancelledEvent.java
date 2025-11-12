package com.services.rental_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a rental is cancelled (FSM: CANCELLED state).
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Car availability reset in car-service</li>
 *   <li>Refund processing</li>
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
public class RentalCancelledEvent extends DomainEvent {

    /**
     * Rental UUID.
     */
    private String rentalId;

    /**
     * Car UUID.
     */
    private String carId;

    /**
     * Reason for cancellation.
     */
    private String cancellationReason;
}
