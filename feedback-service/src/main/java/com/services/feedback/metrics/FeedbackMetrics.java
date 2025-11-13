package com.services.feedback.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom Micrometer metrics for Feedback Service business operations.
 * Provides counters and gauges for monitoring feedback submissions and ratings.
 * 
 * Metrics exported:
 * - feedback.added.total: Counter for feedback submissions
 * - feedback.rating.distribution: Counter for rating distribution (0-5 stars)
 * 
 * @see <a href="https://micrometer.io/docs/concepts">Micrometer Concepts</a>
 */
@Slf4j
@Component
public class FeedbackMetrics {
    
    private static final String SERVICE_TAG = "service";
    private static final String SERVICE_NAME = "feedback-service";
    
    private final Counter feedbackAddedCounter;
    private final MeterRegistry meterRegistry;
    
    public FeedbackMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.feedbackAddedCounter = Counter.builder("feedback.added.total")
                .description("Total number of feedback submissions")
                .tag(SERVICE_TAG, SERVICE_NAME)
                .register(meterRegistry);
    }
    
    public void incrementFeedbackAdded() {
        feedbackAddedCounter.increment();
        log.trace("Incremented feedback.added.total counter");
    }
    
    /**
     * Record rating distribution by star count.
     * 
     * @param rating The rating value (0-5)
     */
    public void recordRating(int rating) {
        meterRegistry.counter("feedback.rating.distribution",
                SERVICE_TAG, SERVICE_NAME,
                "rating", String.valueOf(rating))
                .increment();
        log.trace("Recorded rating distribution: rating={}", rating);
    }
    
    /**
     * Register a gauge for average rating.
     * 
     * @param supplier A function that returns the average rating
     */
    public void registerAverageRatingGauge(java.util.function.Supplier<Number> supplier) {
        io.micrometer.core.instrument.Gauge.builder("feedback.average.rating", supplier)
                .tag(SERVICE_TAG, SERVICE_NAME)
                .description("Average rating across all feedback")
                .register(meterRegistry);
        log.debug("Registered gauge for average rating");
    }
    
    /**
     * Register a gauge for total feedback count.
     * 
     * @param supplier A function that returns the total feedback count
     */
    public void registerTotalFeedbackGauge(java.util.function.Supplier<Number> supplier) {
        io.micrometer.core.instrument.Gauge.builder("feedback.total.count", supplier)
                .tag(SERVICE_TAG, SERVICE_NAME)
                .description("Total feedback count")
                .register(meterRegistry);
        log.debug("Registered gauge for total feedback count");
    }
}
