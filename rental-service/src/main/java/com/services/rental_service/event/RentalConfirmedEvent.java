package com.services.rental_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a rental is confirmed by the car owner (FSM: CONFIRMED state).
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RentalConfirmedEvent extends DomainEvent {

    /**
     * Rental UUID.
     */
    private String rentalId;

    /**
     * Car UUID.
     */
    private String carId;

    /**
     * Renter account ID.
     */
    private String renterId;
}
