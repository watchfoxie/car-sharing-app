package com.services.pricing_rules_service.job;

import com.services.common.kafka.KafkaTopics;
import com.services.common.outbox.OutboxEventStatus;
import com.services.common.outbox.OutboxPoller;
import com.services.common.outbox.OutboxPublishingException;
import com.services.pricing_rules_service.outbox.PricingOutboxEvent;
import com.services.pricing_rules_service.outbox.PricingOutboxEventRepository;
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
 * Scheduled job that polls unpublished pricing outbox events and publishes them to Kafka.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PricingOutboxPollerJob extends OutboxPoller<PricingOutboxEvent> {

    private final PricingOutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Scheduled(fixedDelayString = "${outbox.poller.fixed-delay:5000}",
               initialDelayString = "${outbox.poller.initial-delay:10000}")
    @Transactional
    public void pollAndPublish() {
        super.pollAndPublish();
    }

    @Override
    protected Page<PricingOutboxEvent> findUnpublishedEvents(Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW, pageable);
    }

    @Override
    protected void publishToKafka(String topic, PricingOutboxEvent event) throws OutboxPublishingException {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                    topic,
                    event.getAggregateId(),
                    event.getPayload()
            );

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
    protected String determineTopic(PricingOutboxEvent event) {
        return KafkaTopics.PRICING_EVENTS;
    }

    @Override
    protected void saveEvent(PricingOutboxEvent event) {
        repository.save(event);
    }
}
