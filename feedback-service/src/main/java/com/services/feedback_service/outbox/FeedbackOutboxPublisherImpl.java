package com.services.feedback_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.event.DomainEvent;
import com.services.common.kafka.KafkaSerializationUtil;
import com.services.common.kafka.KafkaTopics;
import com.services.common.outbox.EventSerializationException;
import com.services.common.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of OutboxPublisher for feedback-service.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackOutboxPublisherImpl implements OutboxPublisher<FeedbackOutboxEvent> {

    private final FeedbackOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper = KafkaSerializationUtil.createKafkaObjectMapper();
    
    // Self-injection to enable proper transactional proxy behavior
    @Autowired
    private FeedbackOutboxPublisherImpl self;

    @Override
    @Transactional
    public FeedbackOutboxEvent publish(String aggregateType, String aggregateId, String eventPayload, Map<String, String> headers) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType cannot be null or empty");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId cannot be null or empty");
        }
        if (eventPayload == null || eventPayload.isBlank()) {
            throw new IllegalArgumentException("eventPayload cannot be null or empty");
        }

        FeedbackOutboxEvent outboxEvent = new FeedbackOutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setPayload(eventPayload);
        outboxEvent.setHeaders(headers != null ? headers : new HashMap<>());

        FeedbackOutboxEvent saved = outboxRepository.save(outboxEvent);

        log.debug("Published event to outbox: type={}, aggregateId={}, eventId={}",
                aggregateType, aggregateId, saved.getId());

        return saved;
    }

    @Transactional
    public FeedbackOutboxEvent publishEvent(DomainEvent event) {
        try {
            event.onCreate();

            String payload = objectMapper.writeValueAsString(event);

            Map<String, String> headers = new HashMap<>();
            headers.put(KafkaTopics.HEADER_EVENT_TYPE, event.getEventType());
            headers.put(KafkaTopics.HEADER_SOURCE_SERVICE, "feedback-service");
            headers.put(KafkaTopics.HEADER_CORRELATION_ID,
                    event.getCorrelationId() != null ? event.getCorrelationId() : "N/A");
            headers.put(KafkaTopics.HEADER_EVENT_TIMESTAMP, event.getOccurredAt().toString());

            // Use self-injection to ensure transactional proxy is invoked
            return self.publish(event.getAggregateType(), event.getAggregateId(), payload, headers);

        } catch (Exception e) {
            log.error("Failed to serialize domain event: {}", e.getMessage(), e);
            throw new EventSerializationException("Event serialization failed", e);
        }
    }
}
