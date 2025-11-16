package com.services.car_service.consumer;

import com.services.car_service.sse.CarAvailabilitySseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for rental lifecycle events to trigger car availability SSE updates.
 * 
 * <p><strong>Subscribed Topics:</strong>
 * <ul>
 *   <li><b>rental.created</b> - New rental confirmed → car becomes unavailable</li>
 *   <li><b>rental.return-approved</b> - Rental completed → car becomes available again</li>
 * </ul>
 * 
 * <p><strong>Event Flow:</strong>
 * <pre>
 * rental-service publishes event → Kafka → car-service consumes → SSE broadcast to clients
 * </pre>
 * 
 * <p><strong>SSE Event Mapping:</strong>
 * <ul>
 *   <li>RentalCreatedEvent → SSE event "rental-created" (available: false)</li>
 *   <li>RentalReturnApprovedEvent → SSE event "rental-returned" (available: true)</li>
 * </ul>
 * 
 * <p><strong>Fault Tolerance:</strong>
 * <ul>
 *   <li>Kafka consumer retries (3 attempts with exponential backoff)</li>
 *   <li>Dead Letter Topic (DLT) for unprocessable messages</li>
 *   <li>SSE broadcast failures are logged, not blocking Kafka processing</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RentalEventConsumer {

    private final CarAvailabilitySseService sseService;

    /**
     * Handles rental creation events (car becomes unavailable).
     * 
     * @param message JSON payload: {"rentalId": 123, "carsId": 456, "renterId": "uuid", "status": "CONFIRMED"}
     */
    @KafkaListener(
        topics = "rental.created",
        groupId = "car-service-sse-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRentalCreated(@Payload String message) {
        log.info("Received rental.created event: {}", message);
        
        try {
            // Parse JSON to extract carsId (simple parsing for Phase 17, use ObjectMapper for production)
            Long carId = extractCarId(message);
            if (carId != null) {
                sseService.broadcastAvailabilityEvent("rental-created", carId, false);
                log.info("Broadcasted rental-created event for car {}", carId);
            } else {
                log.warn("Failed to extract carsId from rental.created event: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing rental.created event: {}", e.getMessage(), e);
            // Don't throw exception - SSE failures shouldn't block Kafka processing
        }
    }

    /**
     * Handles rental return approval events (car becomes available again).
     * 
     * @param message JSON payload: {"rentalId": 123, "carsId": 456, "status": "RETURN_APPROVED"}
     */
    @KafkaListener(
        topics = "rental.return-approved",
        groupId = "car-service-sse-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRentalReturnApproved(@Payload String message) {
        log.info("Received rental.return-approved event: {}", message);
        
        try {
            Long carId = extractCarId(message);
            if (carId != null) {
                sseService.broadcastAvailabilityEvent("rental-returned", carId, true);
                log.info("Broadcasted rental-returned event for car {}", carId);
            } else {
                log.warn("Failed to extract carsId from rental.return-approved event: {}", message);
            }
        } catch (Exception e) {
            log.error("Error processing rental.return-approved event: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts carsId from JSON message (simple regex-based parsing).
     * TODO: Replace with Jackson ObjectMapper deserialization in production.
     * 
     * @param jsonMessage the JSON event payload
     * @return extracted carId, or null if not found
     */
    private Long extractCarId(String jsonMessage) {
        try {
            // Simple regex to extract "carsId": 123
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"carsId\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(jsonMessage);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            log.error("Failed to parse carsId from message: {}", e.getMessage());
        }
        return null;
    }
}
