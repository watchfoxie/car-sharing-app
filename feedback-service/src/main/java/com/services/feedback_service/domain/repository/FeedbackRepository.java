package com.services.feedback_service.domain.repository;

import com.services.feedback_service.domain.entity.Feedback;
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
 * JPA Repository for {@link Feedback} entity.
 * 
 * <p>Provides CRUD operations and custom queries for:
 * <ul>
 *   <li>Finding feedback by car with pagination</li>
 *   <li>Aggregation queries (average rating, count per rating)</li>
 *   <li>Duplicate prevention (check if reviewer already submitted feedback for a car)</li>
 *   <li>Anti-abuse rate limiting (count feedback submissions in time window)</li>
 *   <li>Top cars by rating</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Finds all feedback for a specific car with pagination.
     * 
     * @param carsId the car ID
     * @param pageable pagination and sorting parameters
     * @return page of feedback
     */
    Page<Feedback> findByCarsId(Long carsId, Pageable pageable);

    /**
     * Finds all feedback submitted by a specific reviewer with pagination.
     * 
     * @param reviewerId the reviewer account ID
     * @param pageable pagination and sorting parameters
     * @return page of feedback
     */
    Page<Feedback> findByReviewerId(String reviewerId, Pageable pageable);

    /**
     * Checks if a reviewer has already submitted feedback for a specific car.
     * 
     * <p>Used for duplicate prevention.
     * 
     * @param carsId the car ID
     * @param reviewerId the reviewer account ID
     * @return {@code true} if feedback exists
     */
    boolean existsByCarsIdAndReviewerId(Long carsId, String reviewerId);

    /**
     * Finds feedback by car and reviewer (for duplicate check or update).
     * 
     * @param carsId the car ID
     * @param reviewerId the reviewer account ID
     * @return optional feedback
     */
    Optional<Feedback> findByCarsIdAndReviewerId(Long carsId, String reviewerId);

    /**
     * Counts feedback submissions by a reviewer in a time window.
     * 
     * <p>Used for anti-abuse rate limiting (e.g., max 5 feedback per hour).
     * 
     * @param reviewerId the reviewer account ID
     * @param since the start of the time window
     * @return count of feedback submissions
     */
    @Query("SELECT COUNT(f) FROM Feedback f WHERE f.reviewerId = :reviewerId AND f.createdDate >= :since")
    long countByReviewerIdSince(@Param("reviewerId") String reviewerId, @Param("since") Instant since);

    /**
     * Calculates average rating for a specific car.
     * 
     * @param carsId the car ID
     * @return average rating or null if no feedback exists
     */
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.carsId = :carsId")
    Double calculateAverageRating(@Param("carsId") Long carsId);

    /**
     * Counts feedback entries for a specific car.
     * 
     * @param carsId the car ID
     * @return count of feedback
     */
    long countByCarsId(Long carsId);

    /**
     * Finds top N cars by average rating.
     * 
     * <p>Used for reports and recommendations.
     * 
     * @param pageable pagination with limit (e.g., top 10)
     * @return list of [cars_id, avg_rating, count]
     */
    @Query("SELECT f.carsId, AVG(f.rating) as avgRating, COUNT(f) as feedbackCount " +
           "FROM Feedback f " +
           "GROUP BY f.carsId " +
           "HAVING COUNT(f) >= :minFeedbackCount " +
           "ORDER BY AVG(f.rating) DESC")
    List<Object[]> findTopCarsByRating(@Param("minFeedbackCount") long minFeedbackCount, Pageable pageable);

    /**
     * Counts feedback grouped by rating value (for histogram/distribution).
     * 
     * @param carsId the car ID (null for global distribution)
     * @return list of [rating, count]
     */
    @Query("SELECT f.rating, COUNT(f) FROM Feedback f WHERE (:carsId IS NULL OR f.carsId = :carsId) GROUP BY f.rating ORDER BY f.rating DESC")
    List<Object[]> countByRatingDistribution(@Param("carsId") Long carsId);

    /**
     * Finds recent feedback across all cars (for admin dashboard).
     * 
     * @param pageable pagination and sorting
     * @return page of feedback
     */
    Page<Feedback> findAllByOrderByCreatedDateDesc(Pageable pageable);

    /**
     * Counts total feedback submissions by a reviewer (for user profile).
     * 
     * @param reviewerId the reviewer account ID
     * @return count of feedback
     */
    long countByReviewerId(String reviewerId);
}
