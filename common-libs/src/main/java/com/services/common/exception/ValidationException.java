package com.services.common.exception;

import java.util.Map;

/**
 * Exception thrown when validation fails
 */
public class ValidationException extends BusinessException {
    
    private static final String ERROR_CODE = "VALIDATION_ERROR";
    
    private final Map<String, String> errors;
    
    public ValidationException(String message) {
        super(ERROR_CODE, message);
        this.errors = null;
    }
    
    public ValidationException(String message, Map<String, String> errors) {
        super(ERROR_CODE, message);
        this.errors = errors;
    }
    
    public ValidationException(String message, Throwable cause) {
        super(ERROR_CODE, message, cause);
        this.errors = null;
    }
    
    public Map<String, String> getErrors() {
        return errors;
    }
}
