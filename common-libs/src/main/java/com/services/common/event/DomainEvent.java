package com.services.common.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the Car Sharing system.
 * <p>
 * Provides common fields for event identification, ordering, and tracing.
 * All domain events should extend this class to ensure consistent event structure.
 * </p>
 *
 * <p><b>Event Design Principles:</b></p>
 * <ul>
 *   <li><b>Event Notification:</b> Keep payloads small, include only IDs and changed fields</li>
 *   <li><b>Immutability:</b> Events represent facts that happened and should not be modified</li>
 *   <li><b>Versioning:</b> Use the version field for event schema evolution</li>
 *   <li><b>Tracing:</b> Always populate correlationId for distributed tracing</li>
 * </ul>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DomainEvent {

    /**
     * Unique identifier for this event instance.
     * Generated automatically on event creation.
     */
    private UUID eventId;

    /**
     * Identifier of the aggregate root that this event belongs to.
     * Example: Car UUID, Rental UUID, PricingRule UUID
     */
    private String aggregateId;

    /**
     * Type of the aggregate root.
     * Example: "Car", "Rental", "PricingRule", "Feedback"
     */
    private String aggregateType;

    /**
     * Timestamp when the event occurred (in UTC).
     * This is the business timestamp, not the technical publish timestamp.
     */
    private Instant occurredAt;

    /**
     * Correlation ID for distributed tracing.
     * Links this event to the original request that triggered it.
     */
    private String correlationId;

    /**
     * Name of the service that produced this event.
     * Example: "car-service", "rental-service"
     */
    private String sourceService;

    /**
     * Event schema version for backward/forward compatibility.
     * Increment this when making breaking changes to event structure.
     */
    private int version;

    /**
     * Returns the event type name.
     * Default implementation uses the simple class name.
     * Override if custom naming is needed.
     *
     * @return Event type string (e.g., "CarShared", "RentalCreated")
     */
    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Pre-creation initialization.
     * Called before the event is saved to outbox.
     */
    public void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (version == 0) {
            version = 1;
        }
    }
}
