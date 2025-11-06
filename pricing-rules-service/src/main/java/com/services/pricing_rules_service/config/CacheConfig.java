package com.services.pricing_rules_service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Multi-tier caching configuration for Pricing Rules Service.
 * 
 * <p>Implements a <strong>two-tier caching strategy</strong> optimized for pricing rule lookups:</p>
 * <ol>
 *   <li><strong>L1 Cache (Caffeine)</strong>: Local in-memory cache for ultra-fast lookups (µs latency)</li>
 *   <li><strong>L2 Cache (Redis)</strong>: Distributed cache for inter-service sharing (ms latency)</li>
 * </ol>
 * 
 * <p><strong>Cache Regions & TTLs:</strong></p>
 * <table border="1">
 *   <tr>
 *     <th>Cache Name</th>
 *     <th>Purpose</th>
 *     <th>Caffeine TTL</th>
 *     <th>Caffeine Max Size</th>
 *     <th>Redis TTL</th>
 *   </tr>
 *   <tr>
 *     <td>{@code pricingRules}</td>
 *     <td>Active pricing rules by (category, unit)</td>
 *     <td>5 minutes</td>
 *     <td>500 entries</td>
 *     <td>10 minutes</td>
 *   </tr>
 *   <tr>
 *     <td>{@code calculations}</td>
 *     <td>Recent price calculations (optional)</td>
 *     <td>2 minutes</td>
 *     <td>200 entries</td>
 *     <td>5 minutes</td>
 *   </tr>
 * </table>
 * 
 * <p><strong>Cache Flow:</strong></p>
 * <ol>
 *   <li>Request arrives for pricing rule lookup</li>
 *   <li>Check L1 (Caffeine) - if hit, return immediately (µs)</li>
 *   <li>If L1 miss, check L2 (Redis) - if hit, populate L1 and return (ms)</li>
 *   <li>If both miss, query database, populate L2 and L1, return (tens of ms)</li>
 * </ol>
 * 
 * <p><strong>Invalidation Strategy:</strong></p>
 * <ul>
 *   <li><strong>Proactive</strong>: {@code @CacheEvict} on create/update/delete operations</li>
 *   <li><strong>TTL-based</strong>: Caffeine expires locally after 5m, Redis after 10m</li>
 *   <li><strong>Scheduled</strong>: {@code WarmUpCacheJob} pre-populates caches daily</li>
 * </ul>
 * 
 * <p><strong>Performance Characteristics:</strong></p>
 * <ul>
 *   <li><strong>Caffeine hit</strong>: ~1 µs (local memory lookup)</li>
 *   <li><strong>Redis hit</strong>: ~1-5 ms (network round-trip + deserialization)</li>
 *   <li><strong>Database query</strong>: ~10-50 ms (with indexes)</li>
 * </ul>
 * 
 * <p><strong>Caffeine vs Redis Trade-offs:</strong></p>
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>Caffeine (L1)</th>
 *     <th>Redis (L2)</th>
 *   </tr>
 *   <tr>
 *     <td>Latency</td>
 *     <td>Microseconds</td>
 *     <td>Milliseconds</td>
 *   </tr>
 *   <tr>
 *     <td>Capacity</td>
 *     <td>Limited (heap memory)</td>
 *     <td>Large (dedicated server)</td>
 *   </tr>
 *   <tr>
 *     <td>Sharing</td>
 *     <td>Local to JVM</td>
 *     <td>Shared across instances</td>
 *   </tr>
 *   <tr>
 *     <td>Persistence</td>
 *     <td>Lost on restart</td>
 *     <td>Survives restart (if configured)</td>
 *   </tr>
 * </table>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see org.springframework.cache.CacheManager
 * @see com.github.benmanes.caffeine.cache.Caffeine
 * @see org.springframework.data.redis.cache.RedisCacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Composite cache manager combining Caffeine (L1) and Redis (L2).
     * 
     * <p><strong>Lookup Order:</strong></p>
     * <ol>
     *   <li>Caffeine (local, fast)</li>
     *   <li>Redis (distributed, slower but shared)</li>
     * </ol>
     * 
     * <p>The composite manager delegates cache operations to the first manager
     * in the list that can handle the cache name. Since Caffeine is listed first,
     * it always handles lookups before Redis, providing fast local hits.</p>
     * 
     * @param caffeineCacheManager Local in-memory cache manager
     * @param redisCacheManager Distributed cache manager
     * @return Composite cache manager delegating to Caffeine → Redis
     */
    @Bean
    @Primary
    public CacheManager compositeCacheManager(
        CacheManager caffeineCacheManager,
        CacheManager redisCacheManager
    ) {
        CompositeCacheManager compositeCacheManager = new CompositeCacheManager();
        compositeCacheManager.setCacheManagers(
            Arrays.asList(
                caffeineCacheManager, // L1: Check local cache first
                redisCacheManager     // L2: Fall back to Redis
            )
        );
        compositeCacheManager.setFallbackToNoOpCache(false); // Fail if cache not found (strict)
        return compositeCacheManager;
    }

    /**
     * Caffeine local in-memory cache manager (L1).
     * 
     * <p><strong>Configuration per Cache:</strong></p>
     * <ul>
     *   <li><strong>pricingRules</strong>: 500 max entries, 5 min TTL</li>
     *   <li><strong>calculations</strong>: 200 max entries, 2 min TTL</li>
     * </ul>
     * 
     * <p><strong>Eviction Policy:</strong></p>
     * <ul>
     *   <li>LRU (Least Recently Used) when max size is reached</li>
     *   <li>Automatic expiration after TTL (expireAfterWrite)</li>
     * </ul>
     * 
     * @return Caffeine cache manager with configured caches
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // pricingRules cache: Frequently accessed active rules
        cacheManager.registerCustomCache("pricingRules",
            Caffeine.newBuilder()
                .maximumSize(500)                  // Max 500 entries (protects heap)
                .expireAfterWrite(Duration.ofMinutes(5)) // 5 min TTL
                .recordStats()                     // Enable hit/miss metrics
                .build());
        
        // calculations cache: Recent price calculations (optional, for repeated requests)
        cacheManager.registerCustomCache("calculations",
            Caffeine.newBuilder()
                .maximumSize(200)                  // Smaller cache (calculations are less frequent)
                .expireAfterWrite(Duration.ofMinutes(2)) // 2 min TTL (fresher data)
                .recordStats()
                .build());
        
        return cacheManager;
    }

    /**
     * Redis distributed cache manager (L2).
     * 
     * <p><strong>Configuration per Cache:</strong></p>
     * <ul>
     *   <li><strong>pricingRules</strong>: 10 min TTL</li>
     *   <li><strong>calculations</strong>: 5 min TTL</li>
     * </ul>
     * 
     * <p><strong>Serialization:</strong></p>
     * <ul>
     *   <li>Keys: {@code StringRedisSerializer} (human-readable in redis-cli)</li>
     *   <li>Values: {@code GenericJackson2JsonRedisSerializer} (JSON, type-safe)</li>
     * </ul>
     * 
     * <p><strong>Transaction Awareness:</strong></p>
     * <ul>
     *   <li>Cache operations respect Spring {@code @Transactional} boundaries</li>
     *   <li>Cache writes are deferred until transaction commits</li>
     * </ul>
     * 
     * @param redisConnectionFactory Redis connection factory (auto-configured by Spring Boot)
     * @return Redis cache manager with per-cache TTL configuration
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Default Redis cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
            .disableCachingNullValues()       // Do not cache null values (avoid cache pollution)
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer()));

        // Per-cache TTL overrides
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // pricingRules cache: 10 min TTL (longer than Caffeine to serve as backup)
        cacheConfigurations.put("pricingRules",
            defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // calculations cache: 5 min TTL
        cacheConfigurations.put("calculations",
            defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware() // Respect @Transactional boundaries
            .build();
    }
}
