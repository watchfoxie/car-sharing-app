package com.services.pricing_rules_service.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.event.DomainEvent;
import com.services.common.kafka.KafkaSerializationUtil;
import com.services.common.kafka.KafkaTopics;
import com.services.common.outbox.EventSerializationException;
import com.services.common.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of OutboxPublisher for pricing-rules-service.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingOutboxPublisherImpl implements OutboxPublisher<PricingOutboxEvent> {

    private final PricingOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper = KafkaSerializationUtil.createKafkaObjectMapper();
    
    @Override
    @Transactional
    public PricingOutboxEvent publish(String aggregateType, String aggregateId, String eventPayload, Map<String, String> headers) {
        validateOutboxArguments(aggregateType, aggregateId, eventPayload);
        Map<String, String> safeHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
        return persistEvent(aggregateType, aggregateId, eventPayload, safeHeaders);
    }

    @Transactional
    public PricingOutboxEvent publishEvent(DomainEvent event) {
        try {
            event.onCreate();

            String payload = objectMapper.writeValueAsString(event);

            Map<String, String> headers = new HashMap<>();
            headers.put(KafkaTopics.HEADER_EVENT_TYPE, event.getEventType());
            headers.put(KafkaTopics.HEADER_SOURCE_SERVICE, "pricing-rules-service");
            headers.put(KafkaTopics.HEADER_CORRELATION_ID,
                    event.getCorrelationId() != null ? event.getCorrelationId() : "N/A");
            headers.put(KafkaTopics.HEADER_EVENT_TIMESTAMP, event.getOccurredAt().toString());

            return persistEvent(event.getAggregateType(), event.getAggregateId(), payload, headers);

        } catch (Exception e) {
            log.error("Failed to serialize domain event: {}", e.getMessage(), e);
            throw new EventSerializationException("Event serialization failed", e);
        }
    }

    private void validateOutboxArguments(String aggregateType, String aggregateId, String eventPayload) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType cannot be null or empty");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId cannot be null or empty");
        }
        if (eventPayload == null || eventPayload.isBlank()) {
            throw new IllegalArgumentException("eventPayload cannot be null or empty");
        }
    }

    private PricingOutboxEvent persistEvent(String aggregateType, String aggregateId, String eventPayload, Map<String, String> headers) {
        PricingOutboxEvent outboxEvent = new PricingOutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setPayload(eventPayload);
        outboxEvent.setHeaders(headers);

        PricingOutboxEvent saved = outboxRepository.save(outboxEvent);
        log.debug("Published event to outbox: type={}, aggregateId={}, eventId={}", aggregateType, aggregateId, saved.getId());
        return saved;
    }
}
