package com.services.common.outbox;

/**
 * Exception thrown when an outbox event fails to be published to Kafka.
 * <p>
 * This exception wraps underlying Kafka or serialization errors and provides
 * context about the outbox publishing operation failure.
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
public class OutboxPublishingException extends Exception {

    /**
     * Constructs a new OutboxPublishingException with the specified detail message.
     *
     * @param message The detail message
     */
    public OutboxPublishingException(String message) {
        super(message);
    }

    /**
     * Constructs a new OutboxPublishingException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause   The underlying cause of the exception
     */
    public OutboxPublishingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new OutboxPublishingException with the specified cause.
     *
     * @param cause The underlying cause of the exception
     */
    public OutboxPublishingException(Throwable cause) {
        super(cause);
    }
}
