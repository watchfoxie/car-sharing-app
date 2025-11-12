package com.services.rental_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Event published when the renter returns the car (FSM: RETURNED state).
 * <p>
 * Awaits owner approval before final completion.
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
public class RentalReturnedEvent extends DomainEvent {

    /**
     * Rental UUID.
     */
    private String rentalId;

    /**
     * Car UUID.
     */
    private String carId;

    /**
     * Actual return timestamp.
     */
    private LocalDateTime returnedAt;
}
