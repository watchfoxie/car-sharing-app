package com.services.rental.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for Rental Service business operations.
 * Provides counters, timers, and gauges for monitoring rental lifecycle events.
 * 
 * Metrics exported:
 * - rental.created.total: Counter for successful rental creation attempts
 * - rental.creation.failed.total: Counter for failed rental creation attempts  
 * - rental.transition.duration: Timer for FSM state transition latency
 * - rental.calculation.duration: Timer for rental cost calculation latency
 * 
 * @see <a href="https://micrometer.io/docs/concepts">Micrometer Concepts</a>
 */
@Slf4j
@Component
public class RentalMetrics {
    
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_NAME = "rental-service";
    
    private final Counter rentalCreatedCounter;
    private final Timer calculationTimer;
    private final MeterRegistry meterRegistry;
    
    public RentalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Counter for successful rental creations
        this.rentalCreatedCounter = Counter.builder("rental.created.total")
                .description("Total number of successfully created rentals")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
        
        // Timer for cost calculations
        this.calculationTimer = Timer.builder("rental.calculation.duration")
                .description("Time taken to calculate rental cost")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
    }
    
    /**
     * Increment counter when a rental is successfully created.
     */
    public void incrementRentalCreated() {
        rentalCreatedCounter.increment();
        log.trace("Incremented rental.created.total counter");
    }
    
    /**
     * Increment counter when rental creation fails.
     * 
     * @param reason The failure reason (e.g., "CarUnavailable", "ValidationFailed")
     */
    public void incrementRentalCreationFailed(String reason) {
        meterRegistry.counter("rental.creation.failed.total",
                SERVICE_TAG, SERVICE_NAME,
                "reason", reason)
                .increment();
        log.trace("Incremented rental.creation.failed.total counter with reason={}", reason);
    }
    
    /**
     * Record timing for FSM state transition.
     * 
     * @param fromState The source state
     * @param toState The destination state
     * @param durationMillis The transition duration in milliseconds
     */
    public void recordFsmTransition(String fromState, String toState, long durationMillis) {
        meterRegistry.timer("rental.transition.duration",
                SERVICE_TAG, SERVICE_NAME,
                "from", fromState,
                "to", toState)
                .record(java.time.Duration.ofMillis(durationMillis));
        log.trace("Recorded FSM transition duration: {}ms ({}->{})", durationMillis, fromState, toState);
    }
    
    /**
     * Get timer for wrapping calculation operations.
     * 
     * Usage:
     * <pre>
     * Timer.Sample sample = Timer.start(meterRegistry);
     * try {
     *     // perform calculation
     * } finally {
     *     rentalMetrics.recordCalculation(sample);
     * }
     * </pre>
     */
    public void recordCalculation(Timer.Sample sample) {
        sample.stop(calculationTimer);
        log.trace("Recorded rental cost calculation duration");
    }
    
    /**
     * Register a gauge for active rentals by status.
     * Should be called on service startup to register dynamic gauges.
     * 
     * @param status The rental status
     * @param supplier A function that returns the count of rentals with this status
     */
    public void registerActiveRentalsByStatusGauge(String status, java.util.function.Supplier<Number> supplier) {
        io.micrometer.core.instrument.Gauge.builder("rental.active.count", supplier)
                .tag(SERVICE_TAG, SERVICE_NAME)
                .tag("status", status)
                .description("Number of active rentals by status")
                .register(meterRegistry);
        log.debug("Registered gauge for active rentals with status={}", status);
    }
}
