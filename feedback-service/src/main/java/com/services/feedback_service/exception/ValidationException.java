package com.services.feedback_service.exception;

/**
 * Exception thrown when validation fails.
 * 
 * <p>Mapped to HTTP 422 Unprocessable Entity by GlobalExceptionHandler.
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
public class ValidationException extends RuntimeException {
    
    /**
     * Constructs exception with message.
     * 
     * @param message error message
     */
    public ValidationException(String message) {
        super(message);
    }
    
    /**
     * Constructs exception with message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
