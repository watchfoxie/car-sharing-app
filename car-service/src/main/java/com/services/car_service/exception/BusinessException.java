package com.services.car_service.exception;

/**
 * Exception thrown when a business rule is violated.
 * 
 * <p>HTTP status: 400 Bad Request
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public class BusinessException extends RuntimeException {

    /**
     * Constructs a new BusinessException with the specified message.
     *
     * @param message the detail message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructs a new BusinessException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
