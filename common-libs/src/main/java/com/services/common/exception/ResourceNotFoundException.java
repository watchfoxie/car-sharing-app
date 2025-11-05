package com.services.common.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String resource, String identifier) {
        super("NOT_FOUND", String.format("%s not found with identifier: %s", resource, identifier));
    }
    
    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
