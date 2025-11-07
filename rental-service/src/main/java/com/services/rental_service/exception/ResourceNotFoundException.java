package com.services.rental_service.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Mapped to HTTP 404 Not Found by GlobalExceptionHandler.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construct exception with message.
     *
     * @param message error message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Construct exception with message and cause.
     *
     * @param message error message
     * @param cause   underlying cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
