package com.services.rental.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that enriches MDC (Mapped Diagnostic Context) with correlation IDs
 * and user identity for structured logging.
 * 
 * Populates MDC with:
 * - requestId: Unique identifier for each request (generated or from X-Request-ID header)
 * - traceId: Distributed tracing ID (from Micrometer/Zipkin)
 * - spanId: Current span ID (from Micrometer/Zipkin)
 * - userId: Authenticated user ID (from JWT sub claim)
 * 
 * @see <a href="https://logback.qos.ch/manual/mdc.html">Logback MDC</a>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter extends OncePerRequestFilter {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String USER_ID_MDC_KEY = "userId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract or generate request ID
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isEmpty()) {
                requestId = request.getHeader(CORRELATION_ID_HEADER);
            }
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            
            // Extract user ID from JWT if authenticated
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                    && authentication.getPrincipal() instanceof Jwt jwt) {
                String userId = jwt.getSubject();
                if (userId != null) {
                    MDC.put(USER_ID_MDC_KEY, userId);
                }
            }
            
            // Add request ID to response header for client correlation
            response.setHeader(REQUEST_ID_HEADER, requestId);
            
            log.trace("MDC populated: requestId={}, userId={}", requestId, MDC.get(USER_ID_MDC_KEY));
            
            filterChain.doFilter(request, response);
            
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
}
