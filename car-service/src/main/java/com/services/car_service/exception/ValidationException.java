package com.services.car_service.exception;

/**
 * Exception thrown when business validation fails.
 * 
 * <p>HTTP status: 422 Unprocessable Entity
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public class ValidationException extends RuntimeException {

    /**
     * Constructs a new ValidationException with the specified message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Constructs a new ValidationException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
