package com.services.common.exception;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends BusinessException {
    
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super("VALIDATION_ERROR", message, cause);
    }
}
