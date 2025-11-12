package com.services.common.kafka;

/**
 * Constants for Kafka topics used across Car Sharing microservices.
 * <p>
 * This centralized definition ensures consistency in topic naming across all services.
 * Each domain has its own topic for domain events, following the pattern: {domain}-events
 * </p>
 *
 * <p><b>Topic Naming Convention:</b></p>
 * <ul>
 *   <li>Use lowercase with hyphens</li>
 *   <li>Format: {service-name}-events</li>
 *   <li>Dead Letter Queue: {topic-name}-dlq</li>
 * </ul>
 *
 * <p><b>Partitioning Strategy:</b></p>
 * <ul>
 *   <li>All topics use aggregate ID as partition key</li>
 *   <li>This ensures events for the same aggregate maintain order</li>
 *   <li>Recommended partition count: 3-6 based on throughput</li>
 * </ul>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===========================
    // Domain Event Topics
    // ===========================

    /**
     * Topic for Car domain events.
     * Events: CarCreated, CarUpdated, CarDeleted, CarShared, CarUnshared
     */
    public static final String CAR_EVENTS = "car-events";

    /**
     * Topic for Rental domain events.
     * Events: RentalCreated, RentalConfirmed, RentalPickedUp, RentalReturned, 
     *         RentalReturnApproved, RentalCancelled
     */
    public static final String RENTAL_EVENTS = "rental-events";

    /**
     * Topic for Pricing domain events.
     * Events: PricingRuleCreated, PricingRuleUpdated, PricingRuleDeleted
     */
    public static final String PRICING_EVENTS = "pricing-events";

    /**
     * Topic for Feedback domain events.
     * Events: FeedbackAdded, FeedbackDeleted
     */
    public static final String FEEDBACK_EVENTS = "feedback-events";

    // ===========================
    // Dead Letter Queue Topics
    // ===========================

    /**
     * Dead letter queue for failed car-events consumption.
     */
    public static final String CAR_EVENTS_DLQ = "car-events-dlq";

    /**
     * Dead letter queue for failed rental-events consumption.
     */
    public static final String RENTAL_EVENTS_DLQ = "rental-events-dlq";

    /**
     * Dead letter queue for failed pricing-events consumption.
     */
    public static final String PRICING_EVENTS_DLQ = "pricing-events-dlq";

    /**
     * Dead letter queue for failed feedback-events consumption.
     */
    public static final String FEEDBACK_EVENTS_DLQ = "feedback-events-dlq";

    // ===========================
    // Event Type Headers
    // ===========================

    /**
     * Header key for event type classification.
     */
    public static final String HEADER_EVENT_TYPE = "event-type";

    /**
     * Header key for correlation ID (for distributed tracing).
     */
    public static final String HEADER_CORRELATION_ID = "correlation-id";

    /**
     * Header key for source service.
     */
    public static final String HEADER_SOURCE_SERVICE = "source-service";

    /**
     * Header key for event timestamp (ISO-8601 format).
     */
    public static final String HEADER_EVENT_TIMESTAMP = "event-timestamp";
}
