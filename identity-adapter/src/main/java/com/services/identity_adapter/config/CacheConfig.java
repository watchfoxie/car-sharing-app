package com.services.identity_adapter.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Local cache configuration for Identity Adapter Service.
 * 
 * <p><strong>L1 Caching Strategy - Caffeine In-Memory:</strong>
 * <ul>
 *   <li><b>accountProfiles</b> - TTL 5 minutes, max 500 entries (account lookup by ID)</li>
 *   <li><b>accountExistence</b> - TTL 3 minutes, max 300 entries (exists checks)</li>
 * </ul>
 * 
 * <p><strong>Use Cases:</strong>
 * <ul>
 *   <li>Frequent account profile lookups (JWT validation, ownership checks)</li>
 *   <li>Username/email availability checks during registration flows</li>
 *   <li>Reduce database load for read-heavy operations</li>
 * </ul>
 * 
 * <p><strong>Invalidation Strategy:</strong>
 * <ul>
 *   <li>Account profile updates → evict accountProfiles cache entry</li>
 *   <li>Time-based expiration (TTL) handles stale data</li>
 *   <li>No distributed cache (Redis) needed for identity data (low mutation rate)</li>
 * </ul>
 * 
 * <p><strong>Performance Targets:</strong>
 * <ul>
 *   <li>Cache hit rate > 85% for profile lookups</li>
 *   <li>Latency reduction: 10ms (DB) → <1ms (L1 cache)</li>
 *   <li>Memory overhead: ~50MB for 500 entries (AccountProfileResponse ~100KB each)</li>
 * </ul>
 * 
 * <p><strong>Why Caffeine-only (no Redis L2)?</strong>
 * <ul>
 *   <li>Identity data changes infrequently (profile updates are rare)</li>
 *   <li>Single identity-adapter instance (no multi-instance cache coherence needed)</li>
 *   <li>JWT validation already distributed via token expiration</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 * @see com.services.identity_adapter.service.AccountService
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

        // Account profiles (getAccountProfile method)
        cacheManager.registerCustomCache("accountProfiles",
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats() // Enable metrics collection
                .build());

        // Existence checks (accountExists, isUsernameAvailable, isEmailAvailable)
        cacheManager.registerCustomCache("accountExistence",
            Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(Duration.ofMinutes(3))
                .recordStats()
                .build());

        return cacheManager;
    }
}
