package com.services.car_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for Car Service.
 * 
 * <p>Caching strategy:
 * <ul>
 *   <li><b>publicCars</b> - TTL 5 minutes, for public car listings</li>
 *   <li><b>carDetails</b> - TTL 10 minutes, for individual car lookups</li>
 *   <li><b>ownerCars</b> - TTL 2 minutes, for owner-specific car lists</li>
 * </ul>
 * 
 * <p>Invalidation:
 * <ul>
 *   <li>Create/update/delete car → evict all related caches</li>
 *   <li>Shareable/archived flag change → evict publicCars</li>
 *   <li>Future: listen to Kafka events for distributed cache invalidation</li>
 * </ul>
 * 
 * <p>Serialization:
 * <ul>
 *   <li>Key: StringRedisSerializer (simple text keys)</li>
 *   <li>Value: GenericJackson2JsonRedisSerializer (JSON for DTOs)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:5m}")
    private Duration defaultTtl;

    /**
     * Configures Redis cache manager with custom TTLs per cache.
     *
     * @param connectionFactory the Redis connection factory
     * @return the configured cache manager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(defaultTtl)
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues(); // Don't cache null values

        // Per-cache configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Public car listings (frequently accessed, moderate update frequency)
        cacheConfigurations.put("publicCars", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Individual car details (less frequently updated)
        cacheConfigurations.put("carDetails", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Owner-specific car lists (more dynamic, shorter TTL)
        cacheConfigurations.put("ownerCars", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .transactionAware() // Enable cache transaction synchronization
            .build();
    }
}
