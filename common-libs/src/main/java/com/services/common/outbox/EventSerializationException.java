package com.services.common.outbox;

/**
 * Exception thrown when a domain event fails to be serialized to JSON.
 * <p>
 * This exception wraps underlying Jackson or serialization errors and provides
 * context about the event serialization failure.
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
public class EventSerializationException extends RuntimeException {

    /**
     * Constructs a new EventSerializationException with the specified detail message.
     *
     * @param message The detail message
     */
    public EventSerializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new EventSerializationException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause   The underlying cause of the exception
     */
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new EventSerializationException with the specified cause.
     *
     * @param cause The underlying cause of the exception
     */
    public EventSerializationException(Throwable cause) {
        super(cause);
    }
}
