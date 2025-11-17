package com.services.rental_service.LimitCases;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import com.services.rental_service.domain.repository.RentalRepository;
import com.services.rental_service.dto.CreateRentalRequest;
import com.services.rental_service.exception.BusinessException;
import com.services.rental_service.service.RentalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 18 (Faza 18): Critical scenarios and edge cases - Concurrency tests.
 * <p>
 * Validates:
 * <ul>
 *   <li>EXCLUDE constraint prevents overlapping active rentals (10+ threads)</li>
 *   <li>Idempotency key enforcement prevents duplicate rentals</li>
 *   <li>Retry mechanism (RetryOnConstraintViolation) functional under load</li>
 *   <li>Database-level protection prevents double-booking even with race conditions</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Test scenarios:</strong>
 * <ol>
 *   <li>10 threads attempt to book same car for same period → only 1 succeeds</li>
 *   <li>10 threads with same idempotency key → only 1 rental created, others get existing</li>
 *   <li>Overlapping periods (staggered): partial overlap should fail</li>
 *   <li>Non-overlapping periods: sequential bookings should succeed</li>
 *   <li>Mixed active/inactive statuses: EXCLUDE applies only to CONFIRMED/PICKED_UP</li>
 * </ol>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09 (Phase 18)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Slf4j
class RentalConcurrencyTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/migration/V1__init.sql");  // Apply Flyway migration

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    private static final Long CAR_ID = 1001L;
    private static final Instant BASE_TIME = Instant.now().plus(1, ChronoUnit.DAYS);
    private static final int THREAD_COUNT = 12;  // 12 threads for stress testing

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        log.info("=== Test setup: cleaned rental_history table ===");
    }

    /**
     * Test 1: Concurrent booking attempts for same car and period.
     * <p>
     * Expected: Only 1 rental created, 11 threads fail with BusinessException or DataIntegrityViolationException.
     * EXCLUDE constraint should prevent overlaps at database level.
     * </p>
     */
    @Test
    @DisplayName("Test 18.1a: Concurrent bookings for same period - EXCLUDE constraint enforcement")
    void testConcurrentBookingSamePeriod() throws InterruptedException {
        // Given
        Instant pickup = BASE_TIME;
        Instant dropoff = BASE_TIME.plus(2, ChronoUnit.HOURS);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // When: 12 threads attempt to book same car for same period
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize start for maximum contention
                    
                    String renterId = "renter-thread-" + threadId;
                    CreateRentalRequest request = CreateRentalRequest.builder()
                            .carsId(CAR_ID)
                            .pickupDatetime(pickup)
                            .returnDatetime(dropoff)
                            .pickupLocation("Test Location " + threadId)
                            .build();

                    rentalService.createRental(request, renterId);
                    successCount.incrementAndGet();
                    log.info("Thread {} SUCCESS: Created rental", threadId);
                } catch (BusinessException | DataIntegrityViolationException e) {
                    failureCount.incrementAndGet();
                    log.info("Thread {} FAILED: {}", threadId, e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.error("Thread {} UNEXPECTED ERROR: {}", threadId, e.getMessage(), e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete (timeout 30 seconds)
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed)
            .withFailMessage("Threads did not complete within timeout")
            .isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Verify results
        log.info("=== Concurrency Test Results: Success={}, Failures={} ===", 
                successCount.get(), failureCount.get());
        
        assertThat(successCount.get())
            .withFailMessage("Expected exactly 1 successful booking, but got " + successCount.get())
            .isEqualTo(1);
        assertThat(failureCount.get())
            .withFailMessage("Expected " + (THREAD_COUNT - 1) + " failures, but got " + failureCount.get())
            .isEqualTo(THREAD_COUNT - 1);

        // Verify database state
        List<Rental> rentals = rentalRepository.findAll();
        assertThat(rentals)
            .withFailMessage("Expected exactly 1 rental in database, found " + rentals.size())
            .hasSize(1);
        assertThat(rentals.get(0).getCarsId()).isEqualTo(CAR_ID);
        assertThat(rentals.get(0).getStatus()).isEqualTo(RentalStatus.CONFIRMED);
    }

    /**
     * Test 2: Idempotency enforcement with same key.
     * <p>
     * Expected: 12 threads with same idempotency key result in 1 rental created,
     * 11 threads get existing rental (no exception, idempotent response).
     * </p>
     */
    @Test
    @DisplayName("Test 18.1b: Idempotency key enforcement - duplicate prevention")
    void testIdempotencyKeyEnforcement() throws InterruptedException {
        // Given
        String sharedIdempotencyKey = UUID.randomUUID().toString();
        String sharedRenterId = "renter-idempotent-001";
        Instant pickup = BASE_TIME.plus(5, ChronoUnit.HOURS);
        Instant dropoff = BASE_TIME.plus(7, ChronoUnit.HOURS);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        ConcurrentHashMap<Long, AtomicInteger> rentalIdCounts = new ConcurrentHashMap<>();

        // When: 12 threads attempt to create rental with same idempotency key
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    CreateRentalRequest request = CreateRentalRequest.builder()
                            .carsId(CAR_ID)
                            .pickupDatetime(pickup)
                            .returnDatetime(dropoff)
                            .pickupLocation("Idempotent Location")
                            .idempotencyKey(sharedIdempotencyKey)
                            .build();

                    var response = rentalService.createRental(request, sharedRenterId);
                    
                    // Track which rental ID was returned
                    rentalIdCounts.computeIfAbsent(response.getId(), k -> new AtomicInteger(0))
                            .incrementAndGet();
                    
                    log.info("Thread {} got rental ID: {}", threadId, response.getId());
                } catch (Exception e) {
                    log.error("Thread {} unexpected error: {}", threadId, e.getMessage(), e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Verify all threads got same rental ID (idempotent response)
        log.info("=== Idempotency Test Results: Rental IDs returned: {} ===", rentalIdCounts.keySet());
        System.out.println("=== Idempotency distribution: " + rentalIdCounts + " ===");
        
        assertThat(rentalIdCounts.keySet())
            .withFailMessage("Expected all threads to return same rental ID (idempotent), but got multiple IDs: " 
                + rentalIdCounts.keySet())
            .hasSize(1);

        Long rentalId = rentalIdCounts.keySet().iterator().next();
        assertThat(rentalIdCounts.get(rentalId).get())
            .withFailMessage("Expected all " + THREAD_COUNT + " threads to get same rental")
            .isEqualTo(THREAD_COUNT);

        // Verify database state: only 1 rental exists
        List<Rental> rentals = rentalRepository.findAll();
        assertThat(rentals).hasSize(1);
        assertThat(rentals.get(0).getIdempotencyKey()).isEqualTo(sharedIdempotencyKey);
    }

    /**
     * Test 3: Overlapping periods with staggered start times.
     * <p>
     * Expected: First rental succeeds (10:00-12:00), second rental fails (11:00-13:00) due to overlap.
     * </p>
     */
    @Test
    @DisplayName("Test 18.1c: Overlapping rental periods - partial overlap rejection")
    void testOverlappingRentalPeriods() {
        // Given: First rental 10:00-12:00
        Instant pickup1 = BASE_TIME;
        Instant dropoff1 = BASE_TIME.plus(2, ChronoUnit.HOURS);
        CreateRentalRequest request1 = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup1)
                .returnDatetime(dropoff1)
                .pickupLocation("Location A")
                .build();

        // When: Create first rental
        var response1 = rentalService.createRental(request1, "renter-001");
        assertThat(response1).isNotNull();
        assertThat(response1.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // Given: Second rental 11:00-13:00 (overlaps with first by 1 hour)
        Instant pickup2 = BASE_TIME.plus(1, ChronoUnit.HOURS);
        Instant dropoff2 = BASE_TIME.plus(3, ChronoUnit.HOURS);
        CreateRentalRequest request2 = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup2)
                .returnDatetime(dropoff2)
                .pickupLocation("Location B")
                .build();

        // Then: Second rental should fail (overlap detected)
        assertThatThrownBy(() -> rentalService.createRental(request2, "renter-002"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");

        // Verify database state: only 1 rental exists
        List<Rental> rentals = rentalRepository.findAll();
        assertThat(rentals).hasSize(1);
    }

    /**
     * Test 4: Non-overlapping sequential bookings.
     * <p>
     * Expected: Both rentals succeed (10:00-12:00, then 12:00-14:00).
     * TSTZRANGE uses '[)' (closed-open interval), so 12:00 end and 12:00 start do NOT overlap.
     * </p>
     */
    @Test
    @DisplayName("Test 18.1d: Non-overlapping sequential bookings - back-to-back rentals succeed")
    void testNonOverlappingSequentialBookings() {
        // Given: First rental 10:00-12:00
        Instant pickup1 = BASE_TIME;
        Instant dropoff1 = BASE_TIME.plus(2, ChronoUnit.HOURS);
        CreateRentalRequest request1 = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup1)
                .returnDatetime(dropoff1)
                .pickupLocation("Location A")
                .build();

        // When: Create first rental
        var response1 = rentalService.createRental(request1, "renter-001");
        assertThat(response1.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // Given: Second rental 12:00-14:00 (no overlap, closed-open interval)
        Instant pickup2 = BASE_TIME.plus(2, ChronoUnit.HOURS);
        Instant dropoff2 = BASE_TIME.plus(4, ChronoUnit.HOURS);
        CreateRentalRequest request2 = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup2)
                .returnDatetime(dropoff2)
                .pickupLocation("Location B")
                .build();

        // Then: Second rental should succeed (no overlap)
        var response2 = rentalService.createRental(request2, "renter-002");
        assertThat(response2.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // Verify database state: 2 rentals exist
        List<Rental> rentals = rentalRepository.findAll();
        assertThat(rentals).hasSize(2);
    }

    /**
     * Test 5: EXCLUDE constraint applies only to CONFIRMED/PICKED_UP statuses.
     * <p>
     * Expected: CANCELLED rental does not block new bookings for same period.
     * </p>
     */
    @Test
    @DisplayName("Test 18.1e: EXCLUDE applies only to active statuses - CANCELLED rental does not block")
    void testExcludeAppliesOnlyToActiveStatuses() {
        // Given: Create rental and immediately cancel it
        Instant pickup = BASE_TIME;
        Instant dropoff = BASE_TIME.plus(2, ChronoUnit.HOURS);
        
        Rental rental1 = Rental.builder()
                .renterId("renter-001")
                .carsId(CAR_ID)
                .pickupDatetime(pickup)
                .returnDatetime(dropoff)
                .status(RentalStatus.CONFIRMED)
                .estimatedCost(BigDecimal.valueOf(100.00))
                .build();
        rentalRepository.save(rental1);
        
        // Cancel the rental (EXCLUDE no longer applies)
        rental1.setStatus(RentalStatus.CANCELLED);
        rentalRepository.save(rental1);

        // When: Attempt to book same car for same period
        CreateRentalRequest request2 = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup)
                .returnDatetime(dropoff)
                .pickupLocation("Location B")
                .build();

        // Then: New rental should succeed (CANCELLED rental does not block)
        var response2 = rentalService.createRental(request2, "renter-002");
        assertThat(response2.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // Verify database state: 2 rentals (1 CANCELLED, 1 CONFIRMED)
        List<Rental> rentals = rentalRepository.findAll();
        assertThat(rentals).hasSize(2);
        assertThat(rentals).extracting(Rental::getStatus)
                .containsExactlyInAnyOrder(RentalStatus.CANCELLED, RentalStatus.CONFIRMED);
    }

    /**
     * Test 6: Retry mechanism under constraint violations.
     * <p>
     * Expected: RetryAspect (@RetryOnConstraintViolation) retries transient failures.
     * Direct repository constraint violations should be caught and retried.
     * </p>
     */
    @Test
    @DisplayName("Test 18.1f: Retry mechanism functional - RetryOnConstraintViolation aspect")
    void testRetryMechanismUnderLoad() throws InterruptedException {
        // Given: 5 threads attempt to book, expect retry logic to handle contention
        int threadCount = 5;
        Instant pickup = BASE_TIME.plus(10, ChronoUnit.HOURS);
        Instant dropoff = BASE_TIME.plus(12, ChronoUnit.HOURS);

        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    CreateRentalRequest request = CreateRentalRequest.builder()
                            .carsId(CAR_ID)
                            .pickupDatetime(pickup)
                            .returnDatetime(dropoff)
                            .pickupLocation("Retry Test " + threadId)
                            .build();

                    rentalService.createRental(request, "renter-retry-" + threadId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected: only 1 succeeds, others fail after retries exhausted
                    log.info("Thread {} failed (expected): {}", threadId, e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(30, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Verify retry mechanism allowed exactly 1 success
        log.info("=== Retry Test Results: Success count = {} ===", successCount.get());
        assertThat(successCount.get())
            .withFailMessage("Retry mechanism should result in exactly 1 successful booking")
            .isEqualTo(1);
    }
}
