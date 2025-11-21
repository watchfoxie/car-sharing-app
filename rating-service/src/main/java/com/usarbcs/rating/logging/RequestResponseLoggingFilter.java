package com.usarbcs.rating.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final long startTime = System.currentTimeMillis();
        final String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(StringUtils::hasText)
                .orElse(UUID.randomUUID().toString());
        MDC.put("requestId", requestId);
        try {
            log.info("Incoming request [{}] {} from {} | requestId={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr(), requestId);
            filterChain.doFilter(request, response);
        } finally {
            final long duration = System.currentTimeMillis() - startTime;
            log.info("Completed request [{}] {} with status {} in {} ms | requestId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), duration, requestId);
            MDC.remove("requestId");
        }
    }
}
