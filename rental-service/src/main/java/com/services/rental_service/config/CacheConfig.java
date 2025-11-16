package com.services.rental_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Local cache configuration for Rental Service.
 * 
 * <p><strong>L1 Caching Strategy - Caffeine In-Memory:</strong>
 * <ul>
 *   <li><b>activeRentals</b> - TTL 2 minutes, max 200 entries (rentals by status: CONFIRMED, PICKED_UP, RETURNED)</li>
 *   <li><b>rentalDetails</b> - TTL 3 minutes, max 300 entries (individual rental lookups by ID)</li>
 * </ul>
 * 
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Frequent active rental queries for owner dashboards</li>
 *   <li>Pending return approvals polling</li>
 *   <li>Rental status checks during car availability lookups</li>
 * </ul>
 * 
 * <p><strong>Invalidation Strategy:</strong>
 * <ul>
 *   <li>FSM state transitions (CONFIRMED→PICKED_UP→RETURNED→RETURN_APPROVED) → evict all activeRentals cache</li>
 *   <li>Rental creation/cancellation → evict activeRentals + specific rentalDetails entry</li>
 *   <li>Cost updates (estimated/final) → evict specific rentalDetails entry</li>
 * </ul>
 * 
 * <p><strong>Performance Targets:</strong>
 * <ul>
 *   <li>Cache hit rate > 75% for active rental queries</li>
 *   <li>Latency reduction: 8ms (DB with composite index) → <1ms (L1 cache)</li>
 *   <li>Memory overhead: ~30MB for 200 entries (Rental entity ~150KB each)</li>
 * </ul>
 * 
 * <p><strong>Why Caffeine-only (no Redis L2)?</strong>
 * <ul>
 *   <li>Rental data is highly mutable (FSM transitions every few minutes)</li>
 *   <li>Short TTL (2-3m) reduces stale data risk</li>
 *   <li>Kafka events provide eventual consistency across rental-service instances</li>
 *   <li>Active rental queries are already optimized with composite index (cars_id, status, pickup_datetime)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 * @see com.services.rental_service.service.RentalService
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures Caffeine cache manager with two cache regions.
     *
     * @return the configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Active rentals by status (findByStatus, findActiveRentalsByCarsId)
        cacheManager.registerCustomCache("activeRentals",
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats() // Enable metrics collection
                .build());

        // Rental details by ID (getRentalById, findById)
        cacheManager.registerCustomCache("rentalDetails",
            Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(Duration.ofMinutes(3))
                .recordStats()
                .build());

        return cacheManager;
    }
}
