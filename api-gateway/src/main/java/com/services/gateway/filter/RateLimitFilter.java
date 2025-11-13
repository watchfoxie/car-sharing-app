package com.services.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that handles rate limit exceeded responses.
 * Intercepts 429 TOO_MANY_REQUESTS status and enriches response with RFC 7807 Problem Details.
 * Adds rate limit headers for client visibility.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807 Problem Details</a>
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    private static final String X_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String X_RATE_LIMIT_RETRY_AFTER = "X-RateLimit-Retry-After-Seconds";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.defer(() -> {
            if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Rate limit exceeded for path: {}", exchange.getRequest().getPath());
                
                // Add rate limit headers
                exchange.getResponse().getHeaders().add(X_RATE_LIMIT_REMAINING, "0");
                exchange.getResponse().getHeaders().add(X_RATE_LIMIT_RETRY_AFTER, "60");
                
                // Set Content-Type to application/problem+json
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
            }
            return Mono.empty();
        }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
