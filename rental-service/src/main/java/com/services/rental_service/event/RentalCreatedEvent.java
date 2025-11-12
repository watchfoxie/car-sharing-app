package com.services.rental_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when a new rental is created (FSM: PENDING_CONFIRMATION state).
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Car availability update in car-service</li>
 *   <li>Notification to car owner for approval</li>
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
public class RentalCreatedEvent extends DomainEvent {

    /**
     * Rental UUID.
     */
    private String rentalId;

    /**
     * Car UUID being rented.
     */
    private String carId;

    /**
     * Renter account ID.
     */
    private String renterId;

    /**
     * Rental start datetime.
     */
    private LocalDateTime startDate;

    /**
     * Rental end datetime.
     */
    private LocalDateTime endDate;

    /**
     * Total computed price.
     */
    private BigDecimal totalPrice;
}
