package com.services.pricing.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for Pricing Rules Service business operations.
 * Provides counters, timers, and gauges for monitoring pricing calculations and caching.
 * 
 * Metrics exported:
 * - pricing.calculation.total: Counter for successful pricing calculations
 * - pricing.calculation.failed.total: Counter for failed calculations  
 * - pricing.calculation.duration: Timer for calculation latency
 * - pricing.rule.applied.total: Counter for pricing rule applications
 * - cache.hit.count: Counter for cache hits (L1 Caffeine, L2 Redis)
 * - cache.miss.count: Counter for cache misses
 * 
 * @see <a href="https://micrometer.io/docs/concepts">Micrometer Concepts</a>
 */
@Slf4j
@Component
public class PricingMetrics {
    
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_NAME = "pricing-rules-service";
    
    private final Counter calculationCounter;
    private final Counter calculationFailedCounter;
    private final Timer calculationTimer;
    private final MeterRegistry meterRegistry;
    
    public PricingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.calculationCounter = Counter.builder("pricing.calculation.total")
                .description("Total number of successful pricing calculations")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        this.calculationFailedCounter = Counter.builder("pricing.calculation.failed.total")
                .description("Total number of failed pricing calculations")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        this.calculationTimer = Timer.builder("pricing.calculation.duration")
                .description("Time taken to calculate pricing")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
    }
    
    public void incrementCalculation() {
        calculationCounter.increment();
        log.trace("Incremented pricing.calculation.total counter");
    }
    
    public void incrementCalculationFailed(String reason) {
        calculationFailedCounter.increment();
        meterRegistry.counter("pricing.calculation.failed.total",
                SERVICE_TAG, SERVICE_NAME,
                "reason", reason)
                .increment();
        log.trace("Incremented pricing.calculation.failed.total counter with reason={}", reason);
    }
    
    /**
     * Record pricing rule application.
     * 
     * @param ruleId The ID of the applied pricing rule
     */
    public void recordRuleApplied(Long ruleId) {
        meterRegistry.counter("pricing.rule.applied.total",
                SERVICE_TAG, SERVICE_NAME,
                "ruleId", String.valueOf(ruleId))
                .increment();
        log.trace("Recorded pricing rule application: ruleId={}", ruleId);
    }
    
    /**
     * Record cache hit with tier differentiation (L1 Caffeine vs L2 Redis).
     * 
     * @param tier The cache tier ("L1" or "L2")
     * @param region The cache region
     */
    public void recordCacheHit(String tier, String region) {
        meterRegistry.counter("cache.hit.count",
                SERVICE_TAG, SERVICE_NAME,
                "tier", tier,
                "region", region)
                .increment();
        log.trace("Cache HIT recorded: tier={}, region={}", tier, region);
    }
    
    /**
     * Record cache miss.
     * 
     * @param region The cache region
     */
    public void recordCacheMiss(String region) {
        meterRegistry.counter("cache.miss.count",
                SERVICE_TAG, SERVICE_NAME,
                "region", region)
                .increment();
        log.trace("Cache MISS recorded: region={}", region);
    }
    
    /**
     * Record calculation timing.
     */
    public void recordCalculation(Timer.Sample sample) {
        sample.stop(calculationTimer);
        log.trace("Recorded pricing calculation duration");
    }
    
    /**
     * Register a gauge for active pricing rules count.
     * 
     * @param supplier A function that returns the count of active pricing rules
     */
    public void registerActiveRulesGauge(java.util.function.Supplier<Number> supplier) {
        io.micrometer.core.instrument.Gauge.builder("pricing.rules.active.count", supplier)
                .tag(SERVICE_TAG, SERVICE_NAME)
                .description("Number of active pricing rules")
                .register(meterRegistry);
        log.debug("Registered gauge for active pricing rules count");
    }
}
