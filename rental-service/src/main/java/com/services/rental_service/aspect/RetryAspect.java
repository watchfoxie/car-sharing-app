package com.services.rental_service.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * AOP aspect for retrying transactional methods that fail due to database constraint violations.
 * <p>
 * Implements exponential backoff with jitter to handle race conditions in concurrent rental bookings.
 * Specifically targets EXCLUDE constraint violations on {@code rental_period} when multiple clients
 * attempt to book the same car simultaneously.
 * </p>
 * <p>
 * <strong>Retry strategy:</strong>
 * <ul>
 *   <li>Initial backoff: 100ms</li>
 *   <li>Exponential multiplier: 2.0 (doubles each retry)</li>
 *   <li>Jitter: ±25% random variation to prevent thundering herd</li>
 *   <li>Max attempts: 3 (configurable via annotation)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Example retry timeline:</strong>
 * <pre>
 * Attempt 1: fails → wait ~100ms (75-125ms with jitter)
 * Attempt 2: fails → wait ~200ms (150-250ms with jitter)
 * Attempt 3: fails → throw exception
 * </pre>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Aspect
@Component("rentalRetryAspect")
@Slf4j
public class RetryAspect {

    private static final Random RANDOM = new Random();

    /**
     * Retry logic for methods annotated with {@link RetryOnConstraintViolation}.
     * <p>
     * Catches {@link DataIntegrityViolationException} (which wraps PostgreSQL constraint violations)
     * and retries with exponential backoff + jitter.
     * </p>
     *
     * @param joinPoint the method execution join point
     * @param retryAnnotation the retry configuration annotation
     * @return the method result (after successful execution or max retries)
     * @throws Throwable if all retry attempts fail or a non-retryable exception occurs
     */
    @Around("@annotation(retryAnnotation)")
    public Object retryOnConstraintViolation(ProceedingJoinPoint joinPoint, RetryOnConstraintViolation retryAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();

        int maxAttempts = retryAnnotation.maxAttempts();
        long initialBackoffMs = retryAnnotation.initialBackoffMs();
        double backoffMultiplier = retryAnnotation.backoffMultiplier();

        int attempt = 1;
        long backoffMs = initialBackoffMs;

        while (true) {
            try {
                // Attempt method execution
                return joinPoint.proceed();

            } catch (DataIntegrityViolationException ex) {
                // Check if this is a constraint violation (EXCLUDE or unique constraint)
                String errorMessage = ex.getMessage();
                boolean isConstraintViolation = errorMessage != null &&
                        (errorMessage.contains("ex_cars_rental_no_overlap") ||
                         errorMessage.contains("uq_rental_idem") ||
                         errorMessage.contains("constraint"));

                if (!isConstraintViolation || attempt >= maxAttempts) {
                    // Non-retryable or max attempts reached
                    log.error("Method {} failed after {} attempts: {}", methodName, attempt, ex.getMessage());
                    throw ex;
                }

                // Retry with exponential backoff + jitter
                long jitter = (long) (backoffMs * 0.25 * (RANDOM.nextDouble() * 2 - 1)); // ±25%
                long delayMs = backoffMs + jitter;

                log.warn("Method {} failed on attempt {} due to constraint violation. Retrying in {}ms...",
                        methodName, attempt, delayMs);

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }

                // Exponential backoff for next attempt
                backoffMs = (long) (backoffMs * backoffMultiplier);
                attempt++;

            } catch (Throwable throwable) {
                // Non-retryable exception (e.g., validation error, business logic error)
                log.error("Method {} failed with non-retryable exception: {}", methodName, throwable.getMessage());
                throw throwable;
            }
        }
    }
}
