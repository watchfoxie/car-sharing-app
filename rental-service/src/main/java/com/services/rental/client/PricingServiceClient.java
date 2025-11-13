package com.services.rental.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * Client for communicating with Pricing Rules Service.
 * Implements circuit breaker, retry, and timeout patterns using Resilience4j.
 * Provides fallback mechanism for graceful degradation.
 * 
 * @see <a href="https://resilience4j.readme.io/docs/circuitbreaker">Resilience4j Circuit Breaker</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PricingServiceClient {
    
    private final RestClient.Builder restClientBuilder;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    
    private static final String SERVICE_NAME = "pricing-service";
    private static final String PRICING_SERVICE_URL = "http://pricing-rules-service/api/v1/pricing";
    
    /**
     * Calculate rental cost with resilience patterns applied.
     * 
     * @param carId The car ID
     * @param startDate The rental start date
     * @param endDate The rental end date
     * @return The calculated cost, or fallback estimate if service unavailable
     */
    public BigDecimal calculateCost(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(SERVICE_NAME);
        Retry retry = retryRegistry.retry(SERVICE_NAME);
        
        Supplier<BigDecimal> supplier = () -> doCalculateCost(carId, startDate, endDate);
        
        // Apply circuit breaker + retry patterns
        Supplier<BigDecimal> decoratedSupplier = CircuitBreaker
                .decorateSupplier(circuitBreaker, 
                        Retry.decorateSupplier(retry, supplier));
        
        try {
            return decoratedSupplier.get();
        } catch (Exception e) {
            log.error("Failed to calculate cost for carId={} after retries, using fallback", carId, e);
            return fallbackCalculateCost(carId, startDate, endDate);
        }
    }
    
    /**
     * Execute the actual REST call to pricing service.
     */
    private BigDecimal doCalculateCost(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        RestClient restClient = restClientBuilder
                .baseUrl(PRICING_SERVICE_URL)
                .build();
        
        log.debug("Calling pricing service: carId={}, start={}, end={}", carId, startDate, endDate);
        
        PricingRequest request = new PricingRequest(carId, startDate, endDate);
        
        PricingResponse response = restClient.post()
                .uri("/calculate")
                .body(request)
                .retrieve()
                .body(PricingResponse.class);
        
        if (response == null || response.totalCost() == null) {
            throw new IllegalStateException("Invalid response from pricing service");
        }
        
        log.debug("Pricing service response: totalCost={}", response.totalCost());
        return response.totalCost();
    }
    
    /**
     * Fallback method that provides a conservative cost estimate.
     * Used when pricing service is unavailable (circuit open).
     * 
     * Applies simple daily rate: 50 EUR/day * number of days.
     */
    private BigDecimal fallbackCalculateCost(Long carId, LocalDateTime startDate, LocalDateTime endDate) {
        log.warn("Using fallback pricing calculation for carId={}", carId);
        
        long days = java.time.Duration.between(startDate, endDate).toDays();
        if (days < 1) {
            days = 1; // Minimum 1 day
        }
        
        BigDecimal fallbackDailyRate = BigDecimal.valueOf(50.00);
        BigDecimal fallbackCost = fallbackDailyRate.multiply(BigDecimal.valueOf(days));
        
        log.info("Fallback cost calculated: {} EUR for {} days", fallbackCost, days);
        return fallbackCost;
    }
    
    /**
     * Check if circuit breaker is currently open.
     */
    public boolean isCircuitOpen() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(SERVICE_NAME);
        boolean isOpen = circuitBreaker.getState() == CircuitBreaker.State.OPEN;
        log.trace("Circuit breaker state for {}: {}", SERVICE_NAME, circuitBreaker.getState());
        return isOpen;
    }
    
    // DTOs for pricing service communication
    
    public record PricingRequest(Long carId, LocalDateTime startDate, LocalDateTime endDate) {}
    
    public record PricingResponse(BigDecimal totalCost, BigDecimal dailyRate, Long appliedRuleId) {}
}
