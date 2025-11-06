package com.services.car_service.exception;

/**
 * Exception thrown when a requested resource is not found.
 * 
 * <p>HTTP status: 404 Not Found
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new ResourceNotFoundException with the specified message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ResourceNotFoundException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
