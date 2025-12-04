package com.usarbcs.gateway.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final RouterValidator routerValidator;
    private final RateLimitProperties properties;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!routerValidator.isSecured(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String key = resolveKey(exchange.getRequest());
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return chain.filter(exchange);
        }

        long waitSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds();
        if (waitSeconds <= 0) {
            waitSeconds = properties.getRefillPeriod().getSeconds();
        }
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(waitSeconds));
        exchange.getResponse().getHeaders().set("X-RateLimit-Key", key);
        return exchange.getResponse().setComplete();
    }

    private String resolveKey(ServerHttpRequest request) {
        String key = request.getHeaders().getFirst(properties.getHeaderName());
        if (key == null) {
            key = request.getHeaders().getFirst("id");
        }
        if (key == null) {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                key = remoteAddress.getAddress().getHostAddress();
            }
        }
        return key != null ? key : "anonymous";
    }

    private Bucket newBucket(String key) {
        Refill refill = Refill.intervally(properties.getRefillTokens(), properties.getRefillPeriod());
        Bandwidth limit = Bandwidth.classic(properties.getCapacity(), refill);
        return Bucket4j.builder().addLimit(limit).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
