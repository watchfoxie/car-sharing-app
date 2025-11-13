package com.services.car.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis connectivity.
 * Validates that the service can communicate with Redis for caching operations.
 * Tests both connection and basic command execution (PING).
 * 
 * @see <a href="https://redis.io/commands/ping/">Redis PING command</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {
    
    private final RedisConnectionFactory redisConnectionFactory;
    
    @Override
    public Health health() {
        try {
            RedisConnection connection = redisConnectionFactory.getConnection();
            
            try {
                // Execute PING command to verify connectivity
                String pong = connection.ping();
                
                log.trace("Redis health check successful: PING response={}", pong);
                
                return Health.up()
                        .withDetail("response", pong)
                        .withDetail("connection", "active")
                        .build();
                        
            } finally {
                connection.close();
            }
            
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
