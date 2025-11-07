package com.services.rental_service.exception;

/**
 * Exception thrown for business logic violations.
 * <p>
 * Mapped to HTTP 400 Bad Request by GlobalExceptionHandler.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
public class BusinessException extends RuntimeException {

    /**
     * Construct exception with message.
     *
     * @param message error message
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Construct exception with message and cause.
     *
     * @param message error message
     * @param cause   underlying cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
