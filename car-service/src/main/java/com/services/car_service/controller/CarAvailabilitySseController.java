package com.services.car_service.controller;

import com.services.car_service.sse.CarAvailabilitySseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * REST controller for real-time car availability updates via Server-Sent Events (SSE).
 * 
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /v1/cars/availability-stream - Subscribe to car availability events</li>
 * </ul>
 * 
 * <p><strong>Event Types:</strong>
 * <ul>
 *   <li><b>connected</b> - Initial connection confirmation</li>
 *   <li><b>car-shared</b> - Car becomes shareable (availability changes)</li>
 *   <li><b>rental-created</b> - Car becomes unavailable (rental started)</li>
 *   <li><b>rental-returned</b> - Car becomes available (rental ended)</li>
 * </ul>
 * 
 * <p><strong>Client Usage (JavaScript):</strong>
 * <pre>{@code
 * const eventSource = new EventSource('http://localhost:8081/v1/cars/availability-stream?subscriptionId=' + uuid);
 * 
 * eventSource.addEventListener('connected', (e) => {
 *   console.log('Connected:', e.data);
 * });
 * 
 * eventSource.addEventListener('car-shared', (e) => {
 *   const { carId, available, timestamp } = JSON.parse(e.data);
 *   console.log(`Car ${carId} available: ${available}`);
 * });
 * 
 * eventSource.addEventListener('rental-created', (e) => {
 *   const { carId, available } = JSON.parse(e.data);
 *   // Update UI to show car as unavailable
 * });
 * 
 * eventSource.addEventListener('rental-returned', (e) => {
 *   const { carId, available } = JSON.parse(e.data);
 *   // Update UI to show car as available
 * });
 * 
 * eventSource.onerror = () => {
 *   console.error('SSE connection lost, reconnecting...');
 *   eventSource.close();
 *   // Implement exponential backoff reconnect logic
 * };
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 */
@RestController
@RequestMapping("/v1/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Car Availability SSE", description = "Real-time car availability updates via Server-Sent Events")
public class CarAvailabilitySseController {

    private final CarAvailabilitySseService sseService;

    /**
     * Subscribe to real-time car availability updates.
     * 
     * <p><strong>SSE Configuration:</strong>
     * <ul>
     *   <li>Content-Type: text/event-stream</li>
     *   <li>Timeout: 30 minutes</li>
     *   <li>Heartbeat: every 30 seconds</li>
     * </ul>
     * 
     * @param subscriptionId unique subscription ID (optional, UUID generated if not provided)
     * @return SseEmitter for event streaming
     */
    @GetMapping(value = "/availability-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Subscribe to car availability updates",
        description = "Opens a Server-Sent Events (SSE) stream to receive real-time car availability changes. " +
                      "Events are emitted when cars become shareable/unshareable or when rentals are created/returned.",
        responses = {
            @ApiResponse(responseCode = "200", description = "SSE stream established"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (100 req/min)")
        }
    )
    public SseEmitter subscribeToAvailability(
        @Parameter(description = "Unique subscription ID (UUID recommended, auto-generated if not provided)")
        @RequestParam(required = false) String subscriptionId
    ) {
        String finalSubscriptionId = (subscriptionId != null && !subscriptionId.isBlank()) 
            ? subscriptionId 
            : UUID.randomUUID().toString();
        
        log.info("SSE subscription request: subscriptionId={}", finalSubscriptionId);
        
        return sseService.subscribe(finalSubscriptionId);
    }

    /**
     * Get active SSE subscription count (admin/monitoring endpoint).
     * 
     * @return active subscription count
     */
    @GetMapping("/availability-stream/stats")
    @Operation(
        summary = "Get SSE subscription statistics",
        description = "Returns the current number of active SSE subscriptions (for monitoring purposes)."
    )
    public int getSubscriptionStats() {
        return sseService.getActiveSubscriptionCount();
    }
}
