package com.services.feedback_service.exception;

/**
 * Exception thrown for general business rule violations.
 * 
 * <p>Mapped to HTTP 400 Bad Request by GlobalExceptionHandler.
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
public class BusinessException extends RuntimeException {
    
    /**
     * Constructs exception with message.
     * 
     * @param message error message
     */
    public BusinessException(String message) {
        super(message);
    }
    
    /**
     * Constructs exception with message and cause.
     * 
     * @param message error message
     * @param cause underlying cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
