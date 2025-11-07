package com.services.rental_service.domain.enums;

/**
 * Enumeration representing the lifecycle states of a rental in the Finite State Machine (FSM).
 * <p>
 * Valid state transitions:
 * <pre>
 * PENDING → CONFIRMED (after validation)
 * CONFIRMED → PICKED_UP (customer takes possession)
 * CONFIRMED → CANCELLED (before pickup)
 * PICKED_UP → RETURNED (customer returns vehicle)
 * RETURNED → RETURN_APPROVED (operator confirms condition)
 * </pre>
 * </p>
 * <p>
 * Invalid transitions (blocked by business logic):
 * <ul>
 *   <li>RETURNED without prior PICKED_UP</li>
 *   <li>RETURN_APPROVED without prior RETURNED</li>
 *   <li>CANCELLED after PICKED_UP</li>
 * </ul>
 * </p>
 * <p>
 * This enum is synchronized with the PostgreSQL {@code rental_status} ENUM type
 * in the {@code rental} schema.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
public enum RentalStatus {
    /**
     * Initial state when rental request is created, awaiting validation.
     */
    PENDING,

    /**
     * Rental validated and confirmed; vehicle reserved for the specified period.
     * EXCLUDE constraint prevents overlapping rentals in this state.
     */
    CONFIRMED,

    /**
     * Customer has physically picked up the vehicle; rental period started.
     * EXCLUDE constraint prevents overlapping rentals in this state.
     */
    PICKED_UP,

    /**
     * Customer has returned the vehicle; awaiting operator inspection and approval.
     */
    RETURNED,

    /**
     * Operator has approved the return after inspection; rental completed.
     * Final cost calculated, late penalties applied if applicable.
     */
    RETURN_APPROVED,

    /**
     * Rental cancelled by customer or system (only before PICKED_UP).
     * No cost charged if within cancellation_window period.
     */
    CANCELLED
}
