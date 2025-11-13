package com.services.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Configuration for rate limiting key resolvers in Spring Cloud Gateway.
 * Provides different strategies for identifying clients for rate limiting purposes.
 * 
 * Available resolvers:
 * - ipKeyResolver: Rate limit by client IP address
 * - userKeyResolver: Rate limit by authenticated user ID (from JWT)
 * - principalKeyResolver: Rate limit by Spring Security principal
 * 
 * @see <a href="https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#the-redis-ratelimiter">Redis RateLimiter</a>
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Key resolver that uses client IP address for rate limiting.
     * Useful for anonymous endpoints or general traffic throttling.
     * 
     * Note: Consider X-Forwarded-For header if behind a proxy.
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ipAddress = Objects.requireNonNull(
                    exchange.getRequest().getRemoteAddress()
            ).getAddress().getHostAddress();
            
            return Mono.just(ipAddress);
        };
    }
    
    /**
     * Key resolver that uses authenticated user ID from JWT for rate limiting.
     * Provides per-user rate limiting for authenticated endpoints.
     * 
     * Falls back to IP address if user is not authenticated.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .filter(principal -> principal instanceof Authentication)
                .cast(Authentication.class)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(principal -> principal instanceof Jwt)
                .cast(Jwt.class)
                .map(Jwt::getSubject)
                .switchIfEmpty(Mono.defer(() -> {
                    // Fallback to IP address if not authenticated
                    String ipAddress = Objects.requireNonNull(
                            exchange.getRequest().getRemoteAddress()
                    ).getAddress().getHostAddress();
                    return Mono.just("anonymous:" + ipAddress);
                }));
    }
    
    /**
     * Key resolver using Spring Security principal name.
     * Generic approach that works with different authentication mechanisms.
     */
    @Bean
    public KeyResolver principalKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> {
                    if (principal instanceof Authentication auth) {
                        return auth.getName();
                    }
                    return principal.toString();
                })
                .defaultIfEmpty("anonymous");
    }
}
