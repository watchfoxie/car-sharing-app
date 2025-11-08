package com.services.feedback_service.service;

import com.services.feedback_service.domain.repository.FeedbackRepository;
import com.services.feedback_service.dto.RatingDistribution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for feedback reports and analytics.
 * 
 * <p>Provides business intelligence queries:
 * <ul>
 *   <li>Top cars by average rating</li>
 *   <li>Rating distribution (histogram)</li>
 *   <li>Global statistics</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportsService {

    private final FeedbackRepository feedbackRepository;

    /**
     * Retrieves top N cars by average rating.
     * 
     * <p>Filters:
     * <ul>
     *   <li>Minimum feedback count: 3 (to ensure statistically significant ratings)</li>
     *   <li>Sorted by average rating DESC</li>
     * </ul>
     * 
     * @param limit maximum number of cars to return
     * @param minFeedbackCount minimum feedback count for inclusion (default: 3)
     * @return list of [cars_id, avg_rating, feedback_count]
     */
    public List<TopCarRating> getTopCarsByRating(int limit, long minFeedbackCount) {
        log.debug("Retrieving top {} cars by rating (min feedback: {})", limit, minFeedbackCount);
        
        List<Object[]> results = feedbackRepository.findTopCarsByRating(
            minFeedbackCount, 
            PageRequest.of(0, limit)
        );
        
        return results.stream()
            .map(row -> new TopCarRating(
                ((Number) row[0]).longValue(),  // carsId
                ((Number) row[1]).doubleValue(), // avgRating
                ((Number) row[2]).longValue()    // feedbackCount
            ))
            .toList();
    }

    /**
     * Retrieves rating distribution for a specific car or globally.
     * 
     * @param carsId car ID (null for global distribution)
     * @return rating distribution with histogram
     */
    public RatingDistribution getRatingDistribution(Long carsId) {
        log.debug("Retrieving rating distribution for car: {}", carsId);
        
        List<Object[]> results = feedbackRepository.countByRatingDistribution(carsId);
        
        Map<Double, Long> distribution = new HashMap<>();
        long totalCount = 0;
        
        for (Object[] row : results) {
            Double rating = (Double) row[0];
            Long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
            totalCount += count;
        }
        
        return RatingDistribution.builder()
            .carsId(carsId)
            .distribution(distribution)
            .totalCount(totalCount)
            .build();
    }

    /**
     * DTO for top car rating.
     */
    public record TopCarRating(
        Long carsId,
        Double averageRating,
        Long feedbackCount
    ) {}
}
