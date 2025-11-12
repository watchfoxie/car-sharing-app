package com.services.car_service.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.kafka.KafkaSerializationUtil;
import com.services.common.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for feedback events in car-service.
 * <p>
 * Listens to feedback-events topic to invalidate car avgRating cache when:
 * <ul>
 *   <li>New feedback is added → invalidate car cache</li>
 *   <li>Feedback is deleted → invalidate car cache</li>
 * </ul>
 * </p>
 *
 * <p><b>Idempotency:</b> Uses Kafka offset management (manual commit) to ensure exactly-once processing.</p>
 * <p><b>Message-Driven Design:</b> Does not depend on feedback-service classes, only on JSON structure.</p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackEventConsumer {

    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper = KafkaSerializationUtil.createKafkaObjectMapper();

    /**
     * Consumes feedback-added events to invalidate car cache.
     *
     * @param payload        JSON payload
     * @param eventType      Event type header
     * @param acknowledgment Kafka acknowledgment for manual commit
     */
    @KafkaListener(
            topics = KafkaTopics.FEEDBACK_EVENTS,
            groupId = "car-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload String payload,
            @Header(KafkaTopics.HEADER_EVENT_TYPE) String eventType,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received feedback event: type={}", eventType);

            switch (eventType) {
                case "FeedbackAddedEvent":
                    handleFeedbackAdded(payload);
                    break;
                case "FeedbackDeletedEvent":
                    handleFeedbackDeleted(payload);
                    break;
                default:
                    log.warn("Unknown event type: {}", eventType);
            }

            // Manual commit after successful processing
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }

        } catch (Exception e) {
            log.error("Error processing feedback event: {}", e.getMessage(), e);
            // Do NOT acknowledge - message will be redelivered or sent to DLQ
            throw new EventProcessingException("Event processing failed", e);
        }
    }

    /**
     * Handles FeedbackAddedEvent by extracting carId from JSON payload.
     * Expected JSON structure: {"carId": "uuid", "rating": 4, ...}
     */
    private void handleFeedbackAdded(String payload) throws Exception {
        JsonNode eventNode = objectMapper.readTree(payload);
        String carId = eventNode.path("carId").asText(null);
        int rating = eventNode.path("rating").asInt(-1);
        
        log.info("Processing FeedbackAddedEvent: carId={}, rating={}", carId, rating);

        // Invalidate car cache (avgRating needs recalculation)
        invalidateCarCache(carId);
    }

    /**
     * Handles FeedbackDeletedEvent by extracting carId from JSON payload.
     * Expected JSON structure: {"carId": "uuid", ...}
     */
    private void handleFeedbackDeleted(String payload) throws Exception {
        JsonNode eventNode = objectMapper.readTree(payload);
        String carId = eventNode.path("carId").asText(null);
        
        log.info("Processing FeedbackDeletedEvent: carId={}", carId);

        // Invalidate car cache
        invalidateCarCache(carId);
    }

    private void invalidateCarCache(String carId) {
        if (carId == null || carId.isBlank()) {
            log.warn("Cannot invalidate cache: carId is null or empty");
            return;
        }

        var cache = cacheManager.getCache("cars");
        if (cache != null) {
            cache.evict(carId);
            log.debug("Invalidated cache for car: {}", carId);
        }

        // Also invalidate avgRating cache if exists
        var avgRatingCache = cacheManager.getCache("carAvgRating");
        if (avgRatingCache != null) {
            avgRatingCache.evict(carId);
            log.debug("Invalidated avgRating cache for car: {}", carId);
        }
    }
    
    /**
     * Custom exception for event processing failures.
     */
    private static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
