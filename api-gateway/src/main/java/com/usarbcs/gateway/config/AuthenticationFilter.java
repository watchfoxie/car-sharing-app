package com.usarbcs.gateway.config;

import com.usarbcs.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RefreshScope
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {


    private final RouterValidator routerValidator;
    private final JwtUtil tokenHandler;


    @Value("${spring.security.jwt.token-prefix:Bearer}")
    private String tokenPrefix;


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!routerValidator.isSecured(request)) {
            return chain.filter(exchange);
        }

        if (isAuthMissing(request)) {
            return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
        }

        final String authHeader = getAuthHeader(request);
        if (!hasExpectedPrefix(authHeader)) {
            return onError(exchange, "Authorization header must start with " + normalizedPrefix().trim(), HttpStatus.UNAUTHORIZED);
        }

        final String token = extractToken(authHeader);
        if (!StringUtils.hasText(token)) {
            return onError(exchange, "Authorization token is empty", HttpStatus.UNAUTHORIZED);
        }

        try {
            if (tokenHandler.isTokenExpired(token)) {
                return onError(exchange, "Authorization token expired", HttpStatus.UNAUTHORIZED);
            }
            Claims claims = tokenHandler.getAllClaimsFromToken(token);
            ServerWebExchange mutatedExchange = populateRequestWithHeaders(exchange, claims);
            return chain.filter(mutatedExchange);
        } catch (JwtException | IllegalArgumentException ex) {
            return onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, normalizedPrefix().trim());
        response.getHeaders().add("X-Error-Message", err);
        return response.setComplete();
    }

    private String getAuthHeader(ServerHttpRequest request) {
        return request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    }

    private boolean isAuthMissing(ServerHttpRequest request) {
        return !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION);
    }

    private boolean hasExpectedPrefix(String header) {
        return StringUtils.hasText(header) && header.startsWith(normalizedPrefix());
    }

    private String extractToken(String header) {
        return header.substring(normalizedPrefix().length()).trim();
    }

    private String normalizedPrefix() {
        return tokenPrefix.endsWith(" ") ? tokenPrefix : tokenPrefix.trim() + " ";
    }

    private ServerWebExchange populateRequestWithHeaders(ServerWebExchange exchange, Claims claims) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        addClaim(builder, "id", claims.get("id"));
        addClaim(builder, "roles", claims.get("roles"));
        addClaim(builder, "tenantId", claims.get("tenantId"));

        ServerHttpRequest mutatedRequest = builder.build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private void addClaim(ServerHttpRequest.Builder builder, String headerName, Object claimValue) {
        if (claimValue != null) {
            builder.header(headerName, String.valueOf(claimValue));
        }
    }
}
