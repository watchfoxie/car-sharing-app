package com.services.rental_service.controller;

import com.services.rental_service.sse.RentalStatusSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * REST controller for real-time rental status updates via Server-Sent Events (SSE).
 * 
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /v1/rentals/my/status-stream - Subscribe to renter's rental status updates</li>
 *   <li>GET /v1/rentals/owner/status-stream - Subscribe to owner's rental status updates</li>
 * </ul>
 * 
 * <p><strong>Event Types:</strong>
 * <ul>
 *   <li><b>rental-confirmed</b> - Rental validated and confirmed</li>
 *   <li><b>rental-picked-up</b> - Customer took possession</li>
 *   <li><b>rental-returned</b> - Customer returned vehicle</li>
 *   <li><b>rental-approved</b> - Operator approved return</li>
 *   <li><b>rental-cancelled</b> - Rental cancelled</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 */
@RestController
@RequestMapping("/v1/rentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rental Status SSE", description = "Real-time rental status updates via Server-Sent Events")
public class RentalStatusSseController {

    private final RentalStatusSseService sseService;

    /**
     * Subscribe to rental status updates for the authenticated renter.
     * 
     * @param jwt JWT token (contains subject = renterId)
     * @param subscriptionId unique subscription ID (optional)
     * @return SseEmitter for event streaming
     */
    @GetMapping(value = "/my/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Subscribe to renter's rental status updates",
        description = "Opens a Server-Sent Events (SSE) stream to receive real-time status updates for the authenticated user's rentals.",
        security = @SecurityRequirement(name = "oauth2")
    )
    public SseEmitter subscribeAsRenter(
        @AuthenticationPrincipal Jwt jwt,
        @Parameter(description = "Unique subscription ID (auto-generated if not provided)")
        @RequestParam(required = false) String subscriptionId
    ) {
        String renterId = jwt.getSubject();
        String finalSubscriptionId = (subscriptionId != null && !subscriptionId.isBlank()) 
            ? subscriptionId 
            : UUID.randomUUID().toString();
        
        log.info("SSE subscription request (renter): renterId={}, subscriptionId={}", renterId, finalSubscriptionId);
        
        return sseService.subscribeAsRenter(finalSubscriptionId, renterId);
    }

    /**
     * Subscribe to rental status updates for the authenticated car owner.
     * 
     * @param jwt JWT token (contains subject = ownerId)
     * @param subscriptionId unique subscription ID (optional)
     * @return SseEmitter for event streaming
     */
    @GetMapping(value = "/owner/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "Subscribe to owner's rental status updates",
        description = "Opens a Server-Sent Events (SSE) stream to receive real-time status updates for rentals on the authenticated owner's cars.",
        security = @SecurityRequirement(name = "oauth2")
    )
    public SseEmitter subscribeAsOwner(
        @AuthenticationPrincipal Jwt jwt,
        @Parameter(description = "Unique subscription ID (auto-generated if not provided)")
        @RequestParam(required = false) String subscriptionId
    ) {
        String ownerId = jwt.getSubject();
        String finalSubscriptionId = (subscriptionId != null && !subscriptionId.isBlank()) 
            ? subscriptionId 
            : UUID.randomUUID().toString();
        
        log.info("SSE subscription request (owner): ownerId={}, subscriptionId={}", ownerId, finalSubscriptionId);
        
        return sseService.subscribeAsOwner(finalSubscriptionId, ownerId);
    }

    /**
     * Get active SSE subscription count (admin/monitoring endpoint).
     * 
     * @return active subscription count
     */
    @GetMapping("/status-stream/stats")
    @Operation(
        summary = "Get SSE subscription statistics",
        description = "Returns the current number of active SSE subscriptions."
    )
    public int getSubscriptionStats() {
        return sseService.getActiveSubscriptionCount();
    }
}
