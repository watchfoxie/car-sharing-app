package com.services.feedback_service.service;

import com.services.feedback_service.domain.entity.Feedback;
import com.services.feedback_service.domain.repository.FeedbackRepository;
import com.services.feedback_service.dto.CarFeedbackSummary;
import com.services.feedback_service.dto.CreateFeedbackRequest;
import com.services.feedback_service.dto.FeedbackResponse;
import com.services.feedback_service.exception.BusinessException;
import com.services.feedback_service.exception.ResourceNotFoundException;
import com.services.feedback_service.exception.ValidationException;
import com.services.feedback_service.mapper.FeedbackMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for managing feedback operations.
 * 
 * <p>Business logic includes:
 * <ul>
 *   <li>Create feedback with duplicate prevention</li>
 *   <li>Anti-abuse rate limiting (max 5 feedback per hour)</li>
 *   <li>Retrieve feedback by car with aggregations</li>
 *   <li>Retrieve feedback by reviewer</li>
 *   <li>Delete own feedback (soft delete via anonymization)</li>
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
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;

    // Anti-abuse configuration
    private static final int MAX_FEEDBACK_PER_HOUR = 5;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    /**
     * Creates new feedback for a completed rental.
     * 
     * <p>Validations:
     * <ul>
     *   <li>Reviewer has not already submitted feedback for this car (duplicate prevention)</li>
     *   <li>Rate limiting: max 5 feedback per hour per reviewer</li>
     *   <li>TODO: Verify rental exists and is in RETURN_APPROVED status (requires rental-service integration)</li>
     * </ul>
     * 
     * @param request feedback request
     * @param reviewerId the reviewer account ID (from JWT sub claim)
     * @return created feedback
     * @throws ValidationException if duplicate feedback or rate limit exceeded
     */
    @Transactional
    public FeedbackResponse createFeedback(CreateFeedbackRequest request, String reviewerId) {
        log.info("Creating feedback for car {} by reviewer {}", request.getCarsId(), reviewerId);

        // Check duplicate (one feedback per car per reviewer)
        if (feedbackRepository.existsByCarsIdAndReviewerId(request.getCarsId(), reviewerId)) {
            throw new ValidationException(
                String.format("Reviewer %s has already submitted feedback for car %d", reviewerId, request.getCarsId())
            );
        }

        // Check rate limiting (anti-abuse: max 5 feedback per hour)
        Instant rateLimitStart = Instant.now().minus(RATE_LIMIT_WINDOW);
        long recentFeedbackCount = feedbackRepository.countByReviewerIdSince(reviewerId, rateLimitStart);
        if (recentFeedbackCount >= MAX_FEEDBACK_PER_HOUR) {
            throw new BusinessException(
                String.format("Rate limit exceeded: max %d feedback submissions per hour", MAX_FEEDBACK_PER_HOUR)
            );
        }

        // TODO: Verify rental exists and is in RETURN_APPROVED status
        // This requires integration with rental-service (REST call or event-driven)
        // For Phase 7, we'll document this as a future enhancement
        log.warn("TODO: Verify rental {} is in RETURN_APPROVED status (rental-service integration needed)", request.getRentalId());

        // Create feedback entity
        Feedback feedback = feedbackMapper.toEntity(request);
        feedback.setReviewerId(reviewerId);

        // Save feedback
        Feedback savedFeedback = feedbackRepository.save(feedback);
        log.info("Feedback created successfully: id={}, car={}, rating={}", 
            savedFeedback.getId(), savedFeedback.getCarsId(), savedFeedback.getRating());

        return feedbackMapper.toResponse(savedFeedback);
    }

    /**
     * Retrieves feedback by ID.
     * 
     * @param id feedback ID
     * @return feedback response
     * @throws ResourceNotFoundException if feedback not found
     */
    public FeedbackResponse getFeedbackById(Long id) {
        log.debug("Retrieving feedback by id: {}", id);
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));
        return feedbackMapper.toResponse(feedback);
    }

    /**
     * Retrieves all feedback for a specific car with pagination.
     * 
     * @param carsId car ID
     * @param pageable pagination parameters
     * @return page of feedback
     */
    public Page<FeedbackResponse> getFeedbackByCar(Long carsId, Pageable pageable) {
        log.debug("Retrieving feedback for car: {}", carsId);
        return feedbackRepository.findByCarsId(carsId, pageable)
            .map(feedbackMapper::toResponse);
    }

    /**
     * Retrieves feedback summary (aggregations) for a specific car.
     * 
     * <p>Includes:
     * <ul>
     *   <li>Average rating (0.0 to 5.0)</li>
     *   <li>Total feedback count</li>
     * </ul>
     * 
     * @param carsId car ID
     * @return feedback summary
     */
    public CarFeedbackSummary getFeedbackSummaryByCar(Long carsId) {
        log.debug("Calculating feedback summary for car: {}", carsId);
        
        Double avgRating = feedbackRepository.calculateAverageRating(carsId);
        long feedbackCount = feedbackRepository.countByCarsId(carsId);
        
        return CarFeedbackSummary.builder()
            .carsId(carsId)
            .averageRating(avgRating)
            .feedbackCount(feedbackCount)
            .build();
    }

    /**
     * Retrieves all feedback submitted by a specific reviewer with pagination.
     * 
     * @param reviewerId reviewer account ID
     * @param pageable pagination parameters
     * @return page of feedback
     */
    public Page<FeedbackResponse> getFeedbackByReviewer(String reviewerId, Pageable pageable) {
        log.debug("Retrieving feedback by reviewer: {}", reviewerId);
        return feedbackRepository.findByReviewerId(reviewerId, pageable)
            .map(feedbackMapper::toResponse);
    }

    /**
     * Deletes feedback (soft delete: only owner can delete).
     * 
     * <p>Note: Actual deletion is not recommended to preserve data integrity.
     * Instead, consider anonymization (set reviewer_id = NULL) or marking as inactive.
     * 
     * @param id feedback ID
     * @param reviewerId reviewer account ID (for authorization)
     * @throws ResourceNotFoundException if feedback not found
     * @throws ValidationException if user is not the owner
     */
    @Transactional
    public void deleteFeedback(Long id, String reviewerId) {
        log.info("Deleting feedback id={} by reviewer={}", id, reviewerId);
        
        Feedback feedback = feedbackRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Feedback not found with id: " + id));
        
        if (!feedback.isReviewedBy(reviewerId)) {
            throw new ValidationException("You can only delete your own feedback");
        }
        
        feedbackRepository.delete(feedback);
        log.info("Feedback deleted successfully: id={}", id);
    }
}
