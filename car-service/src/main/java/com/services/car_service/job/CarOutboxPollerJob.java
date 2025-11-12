package com.services.car_service.job;

import com.services.car_service.outbox.CarOutboxEvent;
import com.services.car_service.outbox.CarOutboxEventRepository;
import com.services.common.kafka.KafkaTopics;
import com.services.common.outbox.OutboxEventStatus;
import com.services.common.outbox.OutboxPoller;
import com.services.common.outbox.OutboxPublishingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduled job that polls unpublished outbox events and publishes them to Kafka.
 * <p>
 * Runs every 5 seconds (configurable via application.yaml).
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CarOutboxPollerJob extends OutboxPoller<CarOutboxEvent> {

    private final CarOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Scheduled(fixedDelayString = "${outbox.poller.fixed-delay:5000}",
               initialDelayString = "${outbox.poller.initial-delay:10000}")
    @Transactional
    public void pollAndPublish() {
        super.pollAndPublish();
    }

    @Override
    protected Page<CarOutboxEvent> findUnpublishedEvents(Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW, pageable);
    }

    @Override
    protected void publishToKafka(String topic, CarOutboxEvent event) throws OutboxPublishingException {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    topic,
                    event.getAggregateId(), // Use aggregate ID as partition key for ordering
                    event.getPayload()
            );

            // Wait for confirmation (synchronous publish for reliability)
            SendResult<String, String> result = future.get();
            log.debug("Published event to Kafka: topic={}, partition={}, offset={}",
                    topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            throw new OutboxPublishingException("Thread interrupted during Kafka publish", e);
        } catch (Exception e) {
            throw new OutboxPublishingException("Failed to publish to Kafka", e);
        }
    }

    @Override
    protected String determineTopic(CarOutboxEvent event) {
        // All car events go to car-events topic
        return KafkaTopics.CAR_EVENTS;
    }

    @Override
    protected void saveEvent(CarOutboxEvent event) {
        repository.save(event);
    }
}
