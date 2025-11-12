package com.services.car_service.outbox;

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
 * Implementation of OutboxPublisher for car-service.
 * <p>
 * Saves domain events to the car.outbox_event table within the same transaction
 * as the business operation, ensuring no event loss.
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarOutboxPublisherImpl implements OutboxPublisher<CarOutboxEvent> {

    private final CarOutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper = KafkaSerializationUtil.createKafkaObjectMapper();
    
    // Self-injection to enable proper transactional proxy behavior
    @Autowired
    private CarOutboxPublisherImpl self;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public CarOutboxEvent publish(String aggregateType, String aggregateId, String eventPayload, Map<String, String> headers) {
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("aggregateType cannot be null or empty");
        }
        if (aggregateId == null || aggregateId.isBlank()) {
            throw new IllegalArgumentException("aggregateId cannot be null or empty");
        }
        if (eventPayload == null || eventPayload.isBlank()) {
            throw new IllegalArgumentException("eventPayload cannot be null or empty");
        }

        CarOutboxEvent outboxEvent = new CarOutboxEvent();
        outboxEvent.setAggregateType(aggregateType);
        outboxEvent.setAggregateId(aggregateId);
        outboxEvent.setPayload(eventPayload);
        outboxEvent.setHeaders(headers != null ? headers : new HashMap<>());

        CarOutboxEvent saved = outboxRepository.save(outboxEvent);

        log.debug("Published event to outbox: type={}, aggregateId={}, eventId={}",
                aggregateType, aggregateId, saved.getId());

        return saved;
    }

    /**
     * Publishes a domain event by serializing it to JSON.
     *
     * @param event Domain event instance
     * @return Saved outbox event
     */
    @Transactional
    public CarOutboxEvent publishEvent(DomainEvent event) {
        try {
            event.onCreate();

            String payload = objectMapper.writeValueAsString(event);

            Map<String, String> headers = new HashMap<>();
            headers.put(KafkaTopics.HEADER_EVENT_TYPE, event.getEventType());
            headers.put(KafkaTopics.HEADER_SOURCE_SERVICE, "car-service");
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
