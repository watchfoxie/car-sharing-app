package com.services.rental_service.exception;

/**
 * Exception thrown for validation errors.
 * <p>
 * Mapped to HTTP 422 Unprocessable Entity by GlobalExceptionHandler.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
public class ValidationException extends RuntimeException {

    /**
     * Construct exception with message.
     *
     * @param message error message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Construct exception with message and cause.
     *
     * @param message error message
     * @param cause   underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
