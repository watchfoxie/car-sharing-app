package com.services.common.outbox;

import java.util.Map;

/**
 * Interface for publishing domain events to the outbox table.
 * <p>
 * This is the contract that each microservice must implement to reliably publish
 * domain events using the Outbox pattern. Events are first saved to the database
 * in the same transaction as the business operation, ensuring no event loss.
 * </p>
 *
 * <p><b>Implementation Guidelines:</b></p>
 * <ul>
 *   <li>Save events to outbox table within the same transaction as business operations</li>
 *   <li>Use JSON serialization for event payloads</li>
 *   <li>Add tracing/correlation IDs to headers for observability</li>
 *   <li>Keep event payloads small (prefer event notification pattern over event-carried state transfer)</li>
 * </ul>
 *
 * @param <T> The type of outbox event entity (e.g., CarOutboxEvent, RentalOutboxEvent)
 * @author Car Sharing Team
 * @since Phase 12
 */
public interface OutboxPublisher<T extends OutboxEvent> {

    /**
     * Publishes a domain event to the outbox table.
     * <p>
     * The event will be saved with status=NEW and later picked up by the outbox poller job
     * for publishing to Kafka.
     * </p>
     *
     * @param aggregateType Type of the aggregate (e.g., "Car", "Rental")
     * @param aggregateId   Unique identifier of the aggregate instance
     * @param eventPayload  JSON string containing the event data
     * @param headers       Optional metadata/headers (correlation ID, trace ID, event type, etc.)
     * @return The saved outbox event entity
     * @throws IllegalArgumentException if required parameters are null or empty
     */
    T publish(String aggregateType, String aggregateId, String eventPayload, Map<String, String> headers);

    /**
     * Publishes a domain event with minimal parameters (no headers).
     *
     * @param aggregateType Type of the aggregate
     * @param aggregateId   Unique identifier of the aggregate instance
     * @param eventPayload  JSON string containing the event data
     * @return The saved outbox event entity
     */
    default T publish(String aggregateType, String aggregateId, String eventPayload) {
        return publish(aggregateType, aggregateId, eventPayload, null);
    }
}
