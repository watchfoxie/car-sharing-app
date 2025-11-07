package com.services.rental_service.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be retried on database constraint violations.
 * <p>
 * Used specifically for EXCLUDE constraint violations on {@code rental_period} when
 * two concurrent requests attempt to book the same car for overlapping periods.
 * </p>
 * <p>
 * Retry strategy: exponential backoff with jitter.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RetryOnConstraintViolation {

    /**
     * Maximum number of retry attempts.
     * Default: 3
     */
    int maxAttempts() default 3;

    /**
     * Initial backoff delay in milliseconds.
     * Default: 100ms
     */
    long initialBackoffMs() default 100;

    /**
     * Backoff multiplier for exponential backoff.
     * Default: 2.0 (doubles each retry)
     */
    double backoffMultiplier() default 2.0;
}
