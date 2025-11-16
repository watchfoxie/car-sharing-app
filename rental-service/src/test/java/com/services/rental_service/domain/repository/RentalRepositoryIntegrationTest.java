package com.services.rental_service.domain.repository;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link RentalRepository} with PostgreSQL Testcontainers.
 * <p>
 * Tests key database constraints:
 * <ul>
 *   <li>EXCLUDE constraint on rental_period (prevents overlapping active rentals)</li>
 *   <li>Unique constraint on (renter_id, idempotency_key)</li>
 *   <li>CHECK constraints (return_datetime >= pickup_datetime, status-dependent fields)</li>
 *   <li>Concurrency handling with multiple threads</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RentalRepositoryIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private RentalRepository rentalRepository;

    private static final String RENTER_ID = "renter-123";
    private static final Long CAR_ID = 1L;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
    }

    // ========== CRUD Tests ==========

    @Test
    @DisplayName("Should save and retrieve rental successfully")
    void testSaveAndRetrieveRental() {
        // Given
        Rental rental = createSampleRental(RENTER_ID, CAR_ID, NOW, NOW.plus(2, ChronoUnit.HOURS));
        rental.setStatus(RentalStatus.CONFIRMED);

        // When
        Rental saved = rentalRepository.save(rental);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        Optional<Rental> retrieved = rentalRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getRenterId()).isEqualTo(RENTER_ID);
    }

    @Test
    @DisplayName("Should find rental by renter ID and idempotency key")
    void testFindByRenterIdAndIdempotencyKey() {
        // Given
        String idempotencyKey = "idem-key-001";
        Rental rental = createSampleRental(RENTER_ID, CAR_ID, NOW, NOW.plus(2, ChronoUnit.HOURS));
        rental.setIdempotencyKey(idempotencyKey);
        rentalRepository.save(rental);

        // When
        Optional<Rental> found = rentalRepository.findByRenterIdAndIdempotencyKey(RENTER_ID, idempotencyKey);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
    }

    // ========== EXCLUDE Constraint Tests ==========

    @Test
    @DisplayName("EXCLUDE constraint should prevent overlapping CONFIRMED rentals on same car")
    void testExcludeConstraintPreventsOverlappingConfirmed() {
        // Given: First rental (CONFIRMED) for car 1, 10:00-12:00
        Instant pickup1 = NOW;
        Instant return1 = NOW.plus(2, ChronoUnit.HOURS);
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, pickup1, return1);
        rental1.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental1);

        // When/Then: Second rental (CONFIRMED) for same car, overlapping period 11:00-13:00
        Instant pickup2 = NOW.plus(1, ChronoUnit.HOURS);
        Instant return2 = NOW.plus(3, ChronoUnit.HOURS);
        Rental rental2 = createSampleRental("renter-456", CAR_ID, pickup2, return2);
        rental2.setStatus(RentalStatus.CONFIRMED);

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_cars_rental_no_overlap");
    }

    @Test
    @DisplayName("EXCLUDE constraint should prevent overlapping PICKED_UP rentals on same car")
    void testExcludeConstraintPreventsOverlappingPickedUp() {
        // Given: First rental (PICKED_UP) for car 1, 10:00-12:00
        Instant pickup1 = NOW;
        Instant return1 = NOW.plus(2, ChronoUnit.HOURS);
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, pickup1, return1);
        rental1.setStatus(RentalStatus.PICKED_UP);
        rentalRepository.save(rental1);

        // When/Then: Second rental (PICKED_UP) for same car, overlapping period 11:00-13:00
        Instant pickup2 = NOW.plus(1, ChronoUnit.HOURS);
        Instant return2 = NOW.plus(3, ChronoUnit.HOURS);
        Rental rental2 = createSampleRental("renter-456", CAR_ID, pickup2, return2);
        rental2.setStatus(RentalStatus.PICKED_UP);

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_cars_rental_no_overlap");
    }

    @Test
    @DisplayName("EXCLUDE constraint should allow overlapping rentals if status is RETURNED (not active)")
    void testExcludeConstraintAllowsOverlappingReturned() {
        // Given: First rental (RETURNED) for car 1, 10:00-12:00
        Instant pickup1 = NOW;
        Instant return1 = NOW.plus(2, ChronoUnit.HOURS);
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, pickup1, return1);
        rental1.setStatus(RentalStatus.RETURNED);
        rentalRepository.save(rental1);

        // When: Second rental (CONFIRMED) for same car, overlapping period 11:00-13:00
        Instant pickup2 = NOW.plus(1, ChronoUnit.HOURS);
        Instant return2 = NOW.plus(3, ChronoUnit.HOURS);
        Rental rental2 = createSampleRental("renter-456", CAR_ID, pickup2, return2);
        rental2.setStatus(RentalStatus.CONFIRMED);

        // Then: Should succeed (RETURNED rentals don't block availability)
        assertThatCode(() -> rentalRepository.saveAndFlush(rental2))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("EXCLUDE constraint should allow consecutive rentals (no overlap)")
    void testExcludeConstraintAllowsConsecutiveRentals() {
        // Given: First rental (CONFIRMED) for car 1, 10:00-12:00
        Instant pickup1 = NOW;
        Instant return1 = NOW.plus(2, ChronoUnit.HOURS);
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, pickup1, return1);
        rental1.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental1);

        // When: Second rental (CONFIRMED) for same car, starting at 12:00
        Instant pickup2 = return1; // Exactly at return time of first rental
        Instant return2 = pickup2.plus(2, ChronoUnit.HOURS);
        Rental rental2 = createSampleRental("renter-456", CAR_ID, pickup2, return2);
        rental2.setStatus(RentalStatus.CONFIRMED);

        // Then: Should succeed (no overlap, range is [pickup, return) exclusive upper bound)
        assertThatCode(() -> rentalRepository.saveAndFlush(rental2))
                .doesNotThrowAnyException();
    }

    // ========== Idempotency Tests ==========

    @Test
    @DisplayName("Unique constraint should prevent duplicate idempotency keys for same renter")
    void testIdempotencyKeyUniqueness() {
        // Given: First rental with idempotency key
        String idempotencyKey = "idem-key-002";
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, NOW, NOW.plus(2, ChronoUnit.HOURS));
        rental1.setIdempotencyKey(idempotencyKey);
        rentalRepository.save(rental1);

        // When/Then: Second rental with same (renter_id, idempotency_key)
        Rental rental2 = createSampleRental(RENTER_ID, 2L, NOW.plus(5, ChronoUnit.HOURS), NOW.plus(7, ChronoUnit.HOURS));
        rental2.setIdempotencyKey(idempotencyKey);

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_rental_idem");
    }

    @Test
    @DisplayName("Unique constraint should allow same idempotency key for different renters")
    void testIdempotencyKeyAllowedForDifferentRenters() {
        // Given: First rental with idempotency key for renter-123
        String idempotencyKey = "idem-key-003";
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, NOW, NOW.plus(2, ChronoUnit.HOURS));
        rental1.setIdempotencyKey(idempotencyKey);
        rentalRepository.save(rental1);

        // When: Second rental with same idempotency key for different renter
        Rental rental2 = createSampleRental("renter-789", 2L, NOW.plus(5, ChronoUnit.HOURS), NOW.plus(7, ChronoUnit.HOURS));
        rental2.setIdempotencyKey(idempotencyKey);

        // Then: Should succeed (unique constraint is scoped to renter_id)
        assertThatCode(() -> rentalRepository.saveAndFlush(rental2))
                .doesNotThrowAnyException();
    }

    // ========== CHECK Constraint Tests ==========

    @Test
    @DisplayName("CHECK constraint should prevent return_datetime before pickup_datetime")
    void testCheckConstraintReturnAfterPickup() {
        // Given: Rental with return BEFORE pickup
        Instant pickup = NOW.plus(2, ChronoUnit.HOURS);
        Instant returnTime = NOW; // Before pickup
        Rental rental = createSampleRental(RENTER_ID, CAR_ID, pickup, returnTime);

        // When/Then: Should fail
        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_rental_return_after_pickup");
    }

    // ========== Query Method Tests ==========

    @Test
    @DisplayName("Should find active rentals by car ID")
    void testFindActiveRentalsByCarsId() {
        // Given: 3 rentals for car 1 (2 active, 1 returned)
        Rental confirmed = createSampleRental(RENTER_ID, CAR_ID, NOW, NOW.plus(2, ChronoUnit.HOURS));
        confirmed.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(confirmed);

        Rental pickedUp = createSampleRental("renter-456", CAR_ID, NOW.plus(3, ChronoUnit.HOURS), NOW.plus(5, ChronoUnit.HOURS));
        pickedUp.setStatus(RentalStatus.PICKED_UP);
        rentalRepository.save(pickedUp);

        Rental returned = createSampleRental("renter-789", CAR_ID, NOW.plus(6, ChronoUnit.HOURS), NOW.plus(8, ChronoUnit.HOURS));
        returned.setStatus(RentalStatus.RETURNED);
        rentalRepository.save(returned);

        // When
        List<Rental> activeRentals = rentalRepository.findActiveRentalsByCarsId(CAR_ID);

        // Then: Only CONFIRMED and PICKED_UP are returned
        assertThat(activeRentals).hasSize(2);
        assertThat(activeRentals).extracting(Rental::getStatus)
                .containsExactlyInAnyOrder(RentalStatus.CONFIRMED, RentalStatus.PICKED_UP);
    }

    @Test
    @DisplayName("Should count overlapping active rentals")
    void testCountOverlappingActiveRentals() {
        // Given: Active rental for car 1, 10:00-12:00
        Instant pickup1 = NOW;
        Instant return1 = NOW.plus(2, ChronoUnit.HOURS);
        Rental rental1 = createSampleRental(RENTER_ID, CAR_ID, pickup1, return1);
        rental1.setStatus(RentalStatus.CONFIRMED);
        rentalRepository.save(rental1);

        // When: Check for overlapping rental 11:00-13:00
        Instant pickup2 = NOW.plus(1, ChronoUnit.HOURS);
        Instant return2 = NOW.plus(3, ChronoUnit.HOURS);
        long count = rentalRepository.countOverlappingActiveRentals(CAR_ID, pickup2, return2);

        // Then: Should find 1 overlapping rental
        assertThat(count).isEqualTo(1);

        // When: Check for non-overlapping rental 13:00-15:00
        Instant pickup3 = NOW.plus(3, ChronoUnit.HOURS);
        Instant return3 = NOW.plus(5, ChronoUnit.HOURS);
        long count2 = rentalRepository.countOverlappingActiveRentals(CAR_ID, pickup3, return3);

        // Then: Should find 0 overlapping rentals
        assertThat(count2).isZero();
    }

    // ========== Concurrency Tests ==========

    @Test
    @DisplayName("Concurrent rental creation should only allow one to succeed (EXCLUDE constraint)")
    void testConcurrentRentalCreation() throws InterruptedException {
        // Given: 10 threads attempting to book same car for overlapping period
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Instant pickup = NOW.plus(1, ChronoUnit.HOURS);
        Instant returnTime = NOW.plus(3, ChronoUnit.HOURS);

        // When: Launch 10 concurrent rental creations
        for (int i = 0; i < threadCount; i++) {
            String renterId = "renter-" + i;
            executor.submit(() -> {
                try {
                    Rental rental = createSampleRental(renterId, CAR_ID, pickup, returnTime);
                    rental.setStatus(RentalStatus.CONFIRMED);
                    rentalRepository.saveAndFlush(rental);
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    // Expected: EXCLUDE constraint violation
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then: Only 1 rental should succeed, 9 should fail
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);

        // Verify database state
        List<Rental> activeRentals = rentalRepository.findActiveRentalsByCarsId(CAR_ID);
        assertThat(activeRentals).hasSize(1);
    }

    // ========== Helper Methods ==========

    private Rental createSampleRental(String renterId, Long carsId, Instant pickup, Instant returnTime) {
        return Rental.builder()
                .renterId(renterId)
                .carsId(carsId)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .pickupLocation("Location A")
                .returnLocation("Location B")
                .status(RentalStatus.PENDING)
                .estimatedCost(BigDecimal.valueOf(100.00))
                .build();
    }
}
