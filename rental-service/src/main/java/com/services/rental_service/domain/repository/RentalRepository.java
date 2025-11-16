package com.services.rental_service.domain.repository;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for {@link Rental} entities.
 * <p>
 * Provides query methods for rental lifecycle management, conflict detection,
 * and idempotency validation.
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    /**
     * Find rental by idempotency key and renter ID.
     * Used to detect duplicate requests.
     *
     * @param renterId       the renter's account ID
     * @param idempotencyKey the idempotency key
     * @return Optional containing rental if found, empty otherwise
     */
    Optional<Rental> findByRenterIdAndIdempotencyKey(String renterId, String idempotencyKey);

    /**
     * Find all rentals for a specific renter, ordered by pickup datetime descending.
     *
     * @param renterId the renter's account ID
     * @param pageable pagination parameters
     * @return Page of rentals
     */
    Page<Rental> findByRenterIdOrderByPickupDatetimeDesc(String renterId, Pageable pageable);

    /**
     * Find all active rentals (CONFIRMED, PICKED_UP) for a specific car.
     * Used for availability checks.
     *
     * @param carsId the car ID
     * @return List of active rentals (empty if car is available)
     */
    @Query("SELECT r FROM Rental r WHERE r.carsId = :carsId AND r.status IN ('CONFIRMED', 'PICKED_UP')")
    List<Rental> findActiveRentalsByCarsId(@Param("carsId") Long carsId);

    /**
     * Find all rentals for a specific car owned by an operator.
     * Used by operators to view rentals on their vehicles.
     *
     * @param carsId   the car ID
     * @param pageable pagination parameters
     * @return Page of rentals
     */
    Page<Rental> findByCarsIdOrderByPickupDatetimeDesc(Long carsId, Pageable pageable);

    /**
     * Find all rentals in a specific status.
     *
     * @param status   the rental status
     * @param pageable pagination parameters
     * @return Page of rentals
     */
    Page<Rental> findByStatus(RentalStatus status, Pageable pageable);

    /**
     * Count active rentals for a specific car in a given time period.
     * Used for conflict detection (should be 0 for available slots).
     * <p>
     * This query checks for overlapping rentals using range overlap logic:
     * [pickupStart, returnEnd) && [existingPickup, existingReturn)
     * </p>
     *
     * @param carsId        the car ID
     * @param pickupStart   the desired pickup datetime
     * @param returnEnd     the desired return datetime
     * @return count of overlapping active rentals (0 = available, >0 = conflict)
     */
    @Query("SELECT COUNT(r) FROM Rental r WHERE r.carsId = :carsId " +
            "AND r.status IN ('CONFIRMED', 'PICKED_UP') " +
            "AND r.pickupDatetime < :returnEnd " +
            "AND COALESCE(r.returnDatetime, CAST('2100-01-01T00:00:00Z' AS instant)) > :pickupStart")
    long countOverlappingActiveRentals(
            @Param("carsId") Long carsId,
            @Param("pickupStart") Instant pickupStart,
            @Param("returnEnd") Instant returnEnd
    );

    /**
     * Check if an idempotency key exists for a specific renter.
     *
     * @param renterId       the renter's account ID
     * @param idempotencyKey the idempotency key
     * @return true if key exists, false otherwise
     */
    boolean existsByRenterIdAndIdempotencyKey(String renterId, String idempotencyKey);

    /**
     * Find all rentals that are overdue (return_datetime in the past, status PICKED_UP).
     * Used by scheduled jobs to detect late returns.
     *
     * @param now the current timestamp
     * @return List of overdue rentals
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'PICKED_UP' " +
            "AND r.returnDatetime IS NOT NULL AND r.returnDatetime < :now")
    List<Rental> findOverdueRentals(@Param("now") Instant now);

    /**
     * Find all rentals awaiting return approval (status = RETURNED).
     * Used by operators to view pending approvals.
     *
     * @param pageable pagination parameters
     * @return Page of rentals awaiting approval
     */
    @Query("SELECT r FROM Rental r WHERE r.status = 'RETURNED' ORDER BY r.returnDatetime DESC")
    Page<Rental> findPendingReturnApprovals(Pageable pageable);

    /**
     * Count total rentals for a specific car (for analytics).
     *
     * @param carsId the car ID
     * @return count of all rentals
     */
    long countByCarsId(Long carsId);

    /**
     * Count rentals by status (for analytics).
     *
     * @param status the rental status
     * @return count of rentals
     */
    long countByStatus(RentalStatus status);

    /**
     * Find rentals for a specific renter using cursor-based (keyset) pagination.
     * <p>
     * <strong>Keyset Pagination Benefits:</strong>
     * <ul>
     *   <li>Avoids OFFSET performance degradation for large datasets (10k+ rows)</li>
     *   <li>Consistent results during concurrent writes (no phantom reads)</li>
     *   <li>Leverages composite index (idx_history_car_status_pickup) for efficient scanning</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Usage:</strong>
     * First page: pass lastSeenId=null, lastSeenPickupDatetime=null
     * Next pages: pass id and pickup_datetime from last item of previous page
     * </p>
     *
     * @param renterId              the renter's account ID
     * @param lastSeenId            the ID of the last seen rental (null for first page)
     * @param lastSeenPickupDatetime the pickup_datetime of the last seen rental (null for first page)
     * @param pageable              pagination parameters (page size only, page number ignored)
     * @return List of rentals (use .size() == pageable.getPageSize() to check if more pages exist)
     */
    @Query("SELECT r FROM Rental r WHERE r.renterId = :renterId " +
            "AND (:lastSeenPickupDatetime IS NULL OR r.pickupDatetime < :lastSeenPickupDatetime " +
            "     OR (r.pickupDatetime = :lastSeenPickupDatetime AND r.id < :lastSeenId)) " +
            "ORDER BY r.pickupDatetime DESC, r.id DESC")
    List<Rental> findByRenterIdCursor(
            @Param("renterId") String renterId,
            @Param("lastSeenId") Long lastSeenId,
            @Param("lastSeenPickupDatetime") Instant lastSeenPickupDatetime,
            Pageable pageable
    );
}
