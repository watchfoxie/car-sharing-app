package com.services.rental_service.exception;

/**
 * Exception thrown for invalid FSM state transitions.
 * <p>
 * Example: attempting to return a rental that is not in PICKED_UP state.
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
public class InvalidStateTransitionException extends BusinessException {

    /**
     * Construct exception with message.
     *
     * @param message error message describing the invalid transition
     */
    public InvalidStateTransitionException(String message) {
        super(message);
    }

    /**
     * Construct exception with message and cause.
     *
     * @param message error message
     * @param cause   underlying cause
     */
    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
