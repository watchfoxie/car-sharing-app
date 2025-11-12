package com.services.common.outbox;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Base entity for Outbox pattern implementation.
 * <p>
 * Each microservice should extend this class and map it to their specific schema's outbox_event table.
 * This ensures reliable event publishing with at-least-once delivery guarantee.
 * </p>
 *
 * <p><b>Database Schema:</b></p>
 * <pre>
 * CREATE TABLE {schema}.outbox_event (
 *   id UUID PRIMARY KEY,
 *   aggregate_type VARCHAR(100) NOT NULL,
 *   aggregate_id VARCHAR(100) NOT NULL,
 *   payload JSONB NOT NULL,
 *   headers JSONB,
 *   created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 *   published_at TIMESTAMPTZ,
 *   status VARCHAR(20) NOT NULL DEFAULT 'NEW',
 *   CONSTRAINT chk_outbox_status CHECK (status IN ('NEW', 'PUBLISHED', 'FAILED'))
 * );
 * CREATE INDEX idx_outbox_status_created ON {schema}.outbox_event(status, created_at);
 * CREATE INDEX idx_outbox_new ON {schema}.outbox_event(status) WHERE status = 'NEW';
 * </pre>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * &#64;Entity
 * &#64;Table(name = "outbox_event", schema = "car")
 * public class CarOutboxEvent extends OutboxEvent {
 * }
 * </pre>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class OutboxEvent {

    /**
     * Unique identifier for the outbox event.
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Type of the aggregate (e.g., "Car", "Rental", "PricingRule", "Feedback").
     */
    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    /**
     * Identifier of the aggregate instance (e.g., car UUID, rental UUID).
     */
    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    /**
     * Event payload as JSON.
     * Contains the domain event data that will be published to Kafka.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    /**
     * Additional headers/metadata for the event as JSON.
     * Can contain correlation IDs, trace IDs, event type, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", columnDefinition = "jsonb")
    private Map<String, String> headers;

    /**
     * Timestamp when the event was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when the event was successfully published to Kafka.
     * Null if not yet published.
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Current status of the outbox event.
     * <ul>
     *   <li>NEW - Event created but not yet published</li>
     *   <li>PUBLISHED - Event successfully published to Kafka</li>
     *   <li>FAILED - Publishing failed (requires manual intervention or retry)</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    /**
     * Pre-persist callback to generate UUID and set creation timestamp.
     */
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxEventStatus.NEW;
        }
    }

    /**
     * Marks the event as successfully published.
     */
    public void markAsPublished() {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = Instant.now();
    }

    /**
     * Marks the event as failed.
     */
    public void markAsFailed() {
        this.status = OutboxEventStatus.FAILED;
    }
}
