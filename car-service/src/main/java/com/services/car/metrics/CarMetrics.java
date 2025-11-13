package com.services.car.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for Car Service business operations.
 * Provides counters, timers, and gauges for monitoring car management and caching.
 * 
 * Metrics exported:
 * - car.created.total: Counter for car creation events
 * - car.updated.total: Counter for car update events
 * - car.deleted.total: Counter for car deletion events
 * - car.query.duration: Timer for car query latency
 * - cache.hit.count: Counter for cache hits (by region)
 * - cache.miss.count: Counter for cache misses (by region)
 * 
 * @see <a href="https://micrometer.io/docs/concepts">Micrometer Concepts</a>
 */
@Slf4j
@Component
public class CarMetrics {
    
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_NAME = "car-service";
    
    private final Counter carCreatedCounter;
    private final Counter carUpdatedCounter;
    private final Counter carDeletedCounter;
    private final Timer queryTimer;
    private final MeterRegistry meterRegistry;
    
    public CarMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.carCreatedCounter = Counter.builder("car.created.total")
                .description("Total number of cars created")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        this.carUpdatedCounter = Counter.builder("car.updated.total")
                .description("Total number of cars updated")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        this.carDeletedCounter = Counter.builder("car.deleted.total")
                .description("Total number of cars deleted")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        this.queryTimer = Timer.builder("car.query.duration")
                .description("Time taken to query cars")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
    }
    
    public void incrementCarCreated() {
        carCreatedCounter.increment();
        log.trace("Incremented car.created.total counter");
    }
    
    public void incrementCarUpdated() {
        carUpdatedCounter.increment();
        log.trace("Incremented car.updated.total counter");
    }
    
    public void incrementCarDeleted() {
        carDeletedCounter.increment();
        log.trace("Incremented car.deleted.total counter");
    }
    
    /**
     * Record cache hit for a specific cache region.
     * 
     * @param region The cache region (e.g., "publicCars", "carDetails", "ownerCars")
     */
    public void recordCacheHit(String region) {
        meterRegistry.counter("cache.hit.count",
                SERVICE_TAG, SERVICE_NAME,
                "region", region)
                .increment();
        log.trace("Cache HIT recorded for region={}", region);
    }
    
    /**
     * Record cache miss for a specific cache region.
     * 
     * @param region The cache region (e.g., "publicCars", "carDetails", "ownerCars")
     */
    public void recordCacheMiss(String region) {
        meterRegistry.counter("cache.miss.count",
                SERVICE_TAG, SERVICE_NAME,
                "region", region)
                .increment();
        log.trace("Cache MISS recorded for region={}", region);
    }
    
    /**
     * Get timer for wrapping query operations.
     */
    public void recordQuery(Timer.Sample sample) {
        sample.stop(queryTimer);
        log.trace("Recorded car query duration");
    }
    
    /**
     * Register a gauge for public cars count.
     * 
     * @param supplier A function that returns the count of public cars
     */
    public void registerPublicCarsGauge(java.util.function.Supplier<Number> supplier) {
        io.micrometer.core.instrument.Gauge.builder("car.public.count", supplier)
                .tag(SERVICE_TAG, SERVICE_NAME)
                .description("Number of publicly available cars")
                .register(meterRegistry);
        log.debug("Registered gauge for public cars count");
    }
}
