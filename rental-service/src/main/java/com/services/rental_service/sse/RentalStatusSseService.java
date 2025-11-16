package com.services.rental_service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Service for managing Server-Sent Events (SSE) subscriptions to rental status updates.
 * 
 * <p><strong>Real-Time Rental Status Events:</strong>
 * <ul>
 *   <li>PENDING → CONFIRMED (rental validated)</li>
 *   <li>CONFIRMED → PICKED_UP (customer took possession)</li>
 *   <li>PICKED_UP → RETURNED (customer returned vehicle)</li>
 *   <li>RETURNED → RETURN_APPROVED (operator approved return)</li>
 *   <li>CANCELLED (rental cancelled before pickup)</li>
 * </ul>
 * 
 * <p><strong>Subscription Types:</strong>
 * <ul>
 *   <li><b>Renter subscriptions</b> - Filtered by renterId (users see only their rentals)</li>
 *   <li><b>Owner subscriptions</b> - Filtered by car ownership (operators see rentals on their cars)</li>
 * </ul>
 * 
 * <p><strong>SSE Configuration:</strong>
 * <ul>
 *   <li>Timeout: 30 minutes (1,800,000ms)</li>
 *   <li>Heartbeat: every 30 seconds</li>
 *   <li>Per-subscription filters (renterId or ownerId)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 */
@Service
@Slf4j
public class RentalStatusSseService {

    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000; // 30 minutes
    private static final long HEARTBEAT_INTERVAL_SEC = 30; // 30 seconds
    
    private final Map<String, SseSubscription> subscriptions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    public RentalStatusSseService() {
        heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 
            HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    /**
     * Creates a new SSE emitter for renter-specific rental status updates.
     * 
     * @param subscriptionId unique subscription ID
     * @param renterId renter account ID (filter)
     * @return configured SseEmitter
     */
    public SseEmitter subscribeAsRenter(String subscriptionId, String renterId) {
        log.info("New SSE subscription for renter rentals: subscriptionId={}, renterId={}", subscriptionId, renterId);
        return createSubscription(subscriptionId, rental -> rental.contains("\"renterId\":\"" + renterId + "\""));
    }

    /**
     * Creates a new SSE emitter for owner-specific rental status updates.
     * 
     * @param subscriptionId unique subscription ID
     * @param ownerId owner account ID (filter by car ownership)
     * @return configured SseEmitter
     */
    public SseEmitter subscribeAsOwner(String subscriptionId, String ownerId) {
        log.info("New SSE subscription for owner rentals: subscriptionId={}, ownerId={}", subscriptionId, ownerId);
        // TODO: Implement ownerId filter after car-service integration (Phase 18+)
        // For now, accept all rentals (operator dashboard)
        return createSubscription(subscriptionId, rental -> true);
    }

    private SseEmitter createSubscription(String subscriptionId, Predicate<String> filter) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        
        emitter.onCompletion(() -> {
            log.info("SSE subscription completed: {}", subscriptionId);
            subscriptions.remove(subscriptionId);
        });
        
        emitter.onTimeout(() -> {
            log.warn("SSE subscription timed out: {}", subscriptionId);
            subscriptions.remove(subscriptionId);
        });
        
        emitter.onError(ex -> {
            log.error("SSE subscription error for {}: {}", subscriptionId, ex.getMessage());
            subscriptions.remove(subscriptionId);
        });
        
        subscriptions.put(subscriptionId, new SseSubscription(emitter, filter));
        
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Subscribed to rental status updates"));
        } catch (IOException e) {
            log.error("Failed to send initial connection event: {}", e.getMessage());
            subscriptions.remove(subscriptionId);
        }
        
        log.info("Active SSE subscriptions: {}", subscriptions.size());
        return emitter;
    }

    /**
     * Broadcasts a rental status update to all subscribed clients (filtered by renterId/ownerId).
     * 
     * @param eventType event type (e.g., "rental-confirmed", "rental-picked-up", "rental-returned", "rental-approved", "rental-cancelled")
     * @param rentalData JSON data: {"rentalId": 123, "renterId": "uuid", "carsId": 456, "status": "PICKED_UP", "timestamp": "2025-01-15T10:00:00Z"}
     */
    public void broadcastStatusUpdate(String eventType, String rentalData) {
        log.debug("Broadcasting rental status event: type={}, data={}", eventType, rentalData);
        
        int deliveredCount = 0;
        
        for (Map.Entry<String, SseSubscription> entry : subscriptions.entrySet()) {
            String subscriptionId = entry.getKey();
            SseSubscription subscription = entry.getValue();
            
            // Apply filter (check if rental matches subscriber's renterId/ownerId)
            if (!subscription.filter.test(rentalData)) {
                continue; // Skip this subscriber
            }
            
            try {
                subscription.emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(rentalData));
                deliveredCount++;
            } catch (IOException e) {
                log.warn("Failed to send event to subscription {}: {}", subscriptionId, e.getMessage());
                subscriptions.remove(subscriptionId);
            }
        }
        
        log.debug("Event broadcasted to {} subscribers (total subscriptions: {})", deliveredCount, subscriptions.size());
    }

    private void sendHeartbeat() {
        if (subscriptions.isEmpty()) {
            return;
        }
        
        log.trace("Sending heartbeat to {} active SSE connections", subscriptions.size());
        
        subscriptions.forEach((subscriptionId, subscription) -> {
            try {
                subscription.emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException e) {
                log.warn("Heartbeat failed for subscription {}, removing: {}", subscriptionId, e.getMessage());
                subscriptions.remove(subscriptionId);
            }
        });
    }

    public int getActiveSubscriptionCount() {
        return subscriptions.size();
    }

    public void shutdown() {
        log.info("Shutting down RentalStatusSseService, closing {} connections", subscriptions.size());
        heartbeatScheduler.shutdown();
        subscriptions.values().forEach(sub -> sub.emitter.complete());
        subscriptions.clear();
    }

    /**
     * Internal subscription holder with filter predicate.
     */
    private static class SseSubscription {
        private final SseEmitter emitter;
        private final Predicate<String> filter;

        SseSubscription(SseEmitter emitter, Predicate<String> filter) {
            this.emitter = emitter;
            this.filter = filter;
        }
    }
}
