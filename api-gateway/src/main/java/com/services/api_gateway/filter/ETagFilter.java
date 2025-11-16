package com.services.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Global filter for HTTP ETag generation and validation in API Gateway.
 * 
 * <p><strong>ETag Strategy:</strong>
 * <ul>
 *   <li>Weak ETags (W/"hash") for GET requests to cacheable resources</li>
 *   <li>MD5 hash of response body (hex-encoded)</li>
 *   <li>304 Not Modified when If-None-Match matches ETag</li>
 *   <li>Cache-Control headers (max-age=300, public)</li>
 * </ul>
 * 
 * <p><strong>Cacheable Routes:</strong>
 * <ul>
 *   <li>GET /api/v1/cars (public car listings, TTL 5m)</li>
 *   <li>GET /api/v1/feedback/cars/{id}/summary (feedback summaries, TTL 5m)</li>
 *   <li>GET /api/v1/pricing/rules (pricing rules lookup, TTL 5m)</li>
 * </ul>
 * 
 * <p><strong>Non-Cacheable:</strong>
 * <ul>
 *   <li>POST/PUT/DELETE operations (state-changing)</li>
 *   <li>User-specific endpoints (/api/v1/rentals/my, /api/v1/cars/owner/{id})</li>
 *   <li>Real-time SSE streams (/availability-stream, /status-stream)</li>
 * </ul>
 * 
 * <p><strong>Implementation Notes:</strong>
 * <ul>
 *   <li>Buffering response body for hash calculation (memory overhead ~100KB/request)</li>
 *   <li>Filter order = LOWEST_PRECEDENCE - 1 (runs last, after authentication)</li>
 *   <li>Thread-safe MessageDigest instance creation per request</li>
 *   <li>Reactive Mono/Flux for non-blocking I/O</li>
 * </ul>
 * 
 * <p><strong>Performance Targets:</strong>
 * <ul>
 *   <li>ETag generation overhead: < 5ms per request</li>
 *   <li>304 response latency: < 10ms (no downstream call)</li>
 *   <li>Cache hit rate: > 80% for public car listings</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since Phase 17 - Performance Optimizations
 * @see org.springframework.cloud.gateway.filter.GlobalFilter
 */
@Component
public class ETagFilter implements GlobalFilter, Ordered {

    private static final String ETAG_HEADER = "ETag";
    private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String WEAK_ETAG_PREFIX = "W/\"";
    private static final String WEAK_ETAG_SUFFIX = "\"";
    
    // Cacheable route patterns (public, read-only resources)
    private static final String[] CACHEABLE_ROUTES = {
        "/api/v1/cars",
        "/api/v1/feedback/cars/.*/summary",
        "/api/v1/pricing/rules"
    };
    
    // Cache-Control max-age in seconds (5 minutes)
    private static final int MAX_AGE_SECONDS = 300;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Only process GET requests
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        String requestPath = exchange.getRequest().getPath().value();
        
        // Only process cacheable routes
        if (!isCacheableRoute(requestPath)) {
            return chain.filter(exchange);
        }

        // Extract If-None-Match header from request
        String ifNoneMatch = exchange.getRequest().getHeaders().getFirst(IF_NONE_MATCH_HEADER);

        // Decorate response to capture body and generate ETag
        ServerHttpResponse originalResponse = exchange.getResponse();
        ETagResponseDecorator decoratedResponse = new ETagResponseDecorator(originalResponse, ifNoneMatch);

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // Run last (after security, rate limiting, etc.)
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    /**
     * Checks if the request path matches cacheable route patterns.
     *
     * @param path the request path
     * @return true if cacheable, false otherwise
     */
    private boolean isCacheableRoute(String path) {
        for (String pattern : CACHEABLE_ROUTES) {
            if (path.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Custom ServerHttpResponse decorator to intercept response body and generate ETag.
     */
    private static class ETagResponseDecorator extends org.springframework.http.server.reactive.ServerHttpResponseDecorator {

        private final ServerHttpResponse delegate;
        private final String ifNoneMatch;

        public ETagResponseDecorator(ServerHttpResponse delegate, String ifNoneMatch) {
            super(delegate);
            this.delegate = delegate;
            this.ifNoneMatch = ifNoneMatch;
        }

        @Override
        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
            // Only process 200 OK responses
            if (delegate.getStatusCode() != HttpStatus.OK) {
                return super.writeWith(body);
            }

            // Buffer body to calculate ETag
            Flux<DataBuffer> flux = Flux.from(body);
            
            return DataBufferUtils.join(flux)
                .flatMap(dataBuffer -> {
                    try {
                        // Extract body bytes
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        DataBufferUtils.release(dataBuffer);

                        // Generate MD5 hash
                        String etag = generateWeakETag(content);

                        // Check If-None-Match (304 Not Modified)
                        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                            delegate.setStatusCode(HttpStatus.NOT_MODIFIED);
                            delegate.getHeaders().set(ETAG_HEADER, etag);
                            delegate.getHeaders().set(CACHE_CONTROL_HEADER, 
                                "max-age=" + MAX_AGE_SECONDS + ", public");
                            return delegate.setComplete();
                        }

                        // Set ETag and Cache-Control headers
                        delegate.getHeaders().set(ETAG_HEADER, etag);
                        delegate.getHeaders().set(CACHE_CONTROL_HEADER, 
                            "max-age=" + MAX_AGE_SECONDS + ", public");

                        // Write buffered body to response
                        DataBuffer buffer = delegate.bufferFactory().wrap(content);
                        return super.writeWith(Mono.just(buffer));

                    } catch (Exception e) {
                        // Fallback: pass through without ETag on error
                        DataBuffer buffer = delegate.bufferFactory().wrap(new byte[0]);
                        return super.writeWith(Mono.just(buffer));
                    }
                });
        }

        /**
         * Generates a weak ETag (W/"hash") from response body.
         *
         * @param content the response body bytes
         * @return weak ETag string (e.g., W/"5d41402abc4b2a76b9719d911017c592")
         */
        private String generateWeakETag(byte[] content) {
            try {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                byte[] hash = md5.digest(content);
                String hexHash = HexFormat.of().formatHex(hash);
                return WEAK_ETAG_PREFIX + hexHash + WEAK_ETAG_SUFFIX;
            } catch (NoSuchAlgorithmException e) {
                // Fallback to timestamp-based ETag
                return WEAK_ETAG_PREFIX + System.currentTimeMillis() + WEAK_ETAG_SUFFIX;
            }
        }
    }
}
