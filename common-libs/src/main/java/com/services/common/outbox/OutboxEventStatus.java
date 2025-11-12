package com.services.common.outbox;

/**
 * Status of an outbox event in its lifecycle.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
public enum OutboxEventStatus {
    /**
     * Event has been created but not yet published to Kafka.
     * This is the initial state when a domain event is saved to the outbox table.
     */
    NEW,

    /**
     * Event has been successfully published to Kafka.
     * The outbox poller job will update events to this status after successful Kafka delivery.
     */
    PUBLISHED,

    /**
     * Publishing to Kafka failed after retries.
     * These events require manual intervention or will be retried by the poller job.
     */
    FAILED
}
