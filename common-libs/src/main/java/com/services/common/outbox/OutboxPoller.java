package com.services.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for outbox poller jobs.
 * <p>
 * This class implements the polling mechanism that reads unpublished events from the outbox table
 * and publishes them to Kafka. Each microservice should extend this class and provide:
 * <ul>
 *   <li>A repository implementation for fetching outbox events</li>
 *   <li>A Kafka producer for publishing events</li>
 *   <li>Topic routing logic based on event type</li>
 * </ul>
 * </p>
 *
 * <p><b>Scheduling Configuration:</b></p>
 * <pre>
 * &#64;Component
 * &#64;EnableScheduling
 * public class CarOutboxPoller extends OutboxPoller&lt;CarOutboxEvent&gt; {
 *     // Implementation
 * }
 * </pre>
 *
 * <p><b>Guarantees:</b></p>
 * <ul>
 *   <li><b>At-least-once delivery:</b> Events may be published multiple times if failures occur</li>
 *   <li><b>Ordering within partition:</b> Events for the same aggregate ID maintain order in Kafka</li>
 *   <li><b>No event loss:</b> Events survive service restarts due to database persistence</li>
 * </ul>
 *
 * @param <T> The type of outbox event entity
 * @author Car Sharing Team
 * @since Phase 12
 */
public abstract class OutboxPoller<T extends OutboxEvent> {

    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);

    /**
     * Default batch size for processing outbox events.
     * Can be overridden by subclasses based on service load.
     */
    protected static final int DEFAULT_BATCH_SIZE = 100;

    /**
     * Scheduled method that polls for unpublished events and publishes them to Kafka.
     * <p>
     * Runs every 5 seconds by default. Subclasses can override the schedule using their own
     * &#64;Scheduled annotation.
     * </p>
     */
    @Scheduled(fixedDelayString = "${outbox.poller.fixed-delay:5000}",
               initialDelayString = "${outbox.poller.initial-delay:10000}")
    @Transactional
    public void pollAndPublish() {
        try {
            Pageable pageable = PageRequest.of(0, getBatchSize());
            Page<T> unpublishedEvents = findUnpublishedEvents(pageable);

            if (unpublishedEvents.isEmpty()) {
                return;
            }

            log.info("Found {} unpublished outbox events to process", unpublishedEvents.getTotalElements());

            processEvents(unpublishedEvents);

        } catch (Exception e) {
            log.error("Unexpected error during outbox polling: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a page of unpublished outbox events.
     *
     * @param unpublishedEvents Page of events to process
     */
    private void processEvents(Page<T> unpublishedEvents) {
        int successCount = 0;
        int failureCount = 0;

        for (T event : unpublishedEvents.getContent()) {
            try {
                processAndPublishSingleEvent(event);
                successCount++;
            } catch (OutboxPublishingException e) {
                log.error("Failed to publish outbox event [id={}, aggregateType={}, aggregateId={}]: {}",
                        event.getId(), event.getAggregateType(), event.getAggregateId(), e.getMessage(), e);
                event.markAsFailed();
                saveEvent(event);
                failureCount++;
            }
        }

        log.info("Outbox polling completed: {} successful, {} failed", successCount, failureCount);
    }

    /**
     * Processes and publishes a single outbox event.
     *
     * @param event Event to process
     * @throws OutboxPublishingException if publishing fails
     */
    private void processAndPublishSingleEvent(T event) throws OutboxPublishingException {
        try {
            String topic = determineTopic(event);
            publishToKafka(topic, event);
            event.markAsPublished();
            saveEvent(event);
        } catch (Exception e) {
            throw new OutboxPublishingException("Failed to publish event to Kafka", e);
        }
    }

    /**
     * Finds unpublished events (status = NEW) from the outbox table.
     *
     * @param pageable Pagination parameters
     * @return Page of unpublished events ordered by creation timestamp
     */
    protected abstract Page<T> findUnpublishedEvents(Pageable pageable);

    /**
     * Publishes an event to the specified Kafka topic.
     * <p>
     * Implementation should use the aggregate ID as the Kafka partition key to maintain ordering
     * for events belonging to the same aggregate.
     * </p>
     *
     * @param topic Kafka topic name
     * @param event Outbox event to publish
     * @throws OutboxPublishingException if publishing fails
     */
    protected abstract void publishToKafka(String topic, T event) throws OutboxPublishingException;

    /**
     * Determines the target Kafka topic based on event metadata.
     * <p>
     * Typical implementation uses the headers map to extract event type and route to appropriate topic.
     * </p>
     *
     * @param event Outbox event
     * @return Kafka topic name
     */
    protected abstract String determineTopic(T event);

    /**
     * Persists the updated event back to the database.
     *
     * @param event Outbox event with updated status
     */
    protected abstract void saveEvent(T event);

    /**
     * Returns the batch size for processing events in one polling cycle.
     * <p>
     * Default is 100. Override to customize based on service load.
     * </p>
     *
     * @return Batch size
     */
    protected int getBatchSize() {
        return DEFAULT_BATCH_SIZE;
    }
}
