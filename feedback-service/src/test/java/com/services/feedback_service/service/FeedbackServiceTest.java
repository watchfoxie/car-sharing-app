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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FeedbackService}.
 * 
 * <p>Tests feedback management operations including anti-abuse mechanisms
 * using Mockito to isolate service layer from repository and mapper dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>Feedback creation with duplicate prevention</li>
 *   <li>Anti-abuse rate limiting (max 5 feedback/hour)</li>
 *   <li>Feedback retrieval (by ID, car, reviewer)</li>
 *   <li>Aggregation queries (average rating, feedback count)</li>
 *   <li>Soft delete with ownership validation</li>
 *   <li>Edge cases (unauthorized deletion, rate limit breaches)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService Unit Tests")
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private FeedbackMapper feedbackMapper;

    @InjectMocks
    private FeedbackService feedbackService;

    private Feedback testFeedback;
    private FeedbackResponse testResponse;
    private CreateFeedbackRequest createRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testFeedback = Feedback.builder()
                .id(1L)
                .carsId(100L)
                .reviewerId("auth0|reviewer123")
                .rating(4.5)
                .comment("Great car, smooth ride!")
                .build();

        testResponse = FeedbackResponse.builder()
                .id(1L)
                .carsId(100L)
                .reviewerId("auth0|reviewer123")
                .rating(4.5)
                .comment("Great car, smooth ride!")
                .build();

        createRequest = CreateFeedbackRequest.builder()
                .carsId(100L)
                .rentalId(50L)
                .rating(4.5)
                .comment("Great car, smooth ride!")
                .build();

        pageable = PageRequest.of(0, 20);
    }

    // ========================== CREATE FEEDBACK ==========================

    @Test
    @DisplayName("createFeedback - Should create feedback successfully")
    void createFeedback_WhenValid_ShouldCreateFeedback() {
        // Given
        String reviewerId = "auth0|reviewer123";
        when(feedbackRepository.existsByCarsIdAndReviewerId(anyLong(), anyString())).thenReturn(false);
        when(feedbackRepository.countByReviewerIdSince(anyString(), any(Instant.class))).thenReturn(0L);
        when(feedbackMapper.toEntity(createRequest)).thenReturn(testFeedback);
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);
        when(feedbackMapper.toResponse(testFeedback)).thenReturn(testResponse);

        // When
        FeedbackResponse result = feedbackService.createFeedback(createRequest, reviewerId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRating()).isEqualTo(4.5);

        verify(feedbackRepository, times(1)).existsByCarsIdAndReviewerId(100L, reviewerId);
        verify(feedbackRepository, times(1)).countByReviewerIdSince(eq(reviewerId), any(Instant.class));
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    @DisplayName("createFeedback - Should throw ValidationException when duplicate feedback")
    void createFeedback_WhenDuplicate_ShouldThrowValidationException() {
        // Given
        String reviewerId = "auth0|reviewer123";
        when(feedbackRepository.existsByCarsIdAndReviewerId(100L, reviewerId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> feedbackService.createFeedback(createRequest, reviewerId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("has already submitted feedback");

        verify(feedbackRepository, times(1)).existsByCarsIdAndReviewerId(100L, reviewerId);
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    @Test
    @DisplayName("createFeedback - Should throw BusinessException when rate limit exceeded")
    void createFeedback_WhenRateLimitExceeded_ShouldThrowBusinessException() {
        // Given: Reviewer has already submitted 5 feedback in the past hour
        String reviewerId = "auth0|reviewer123";
        when(feedbackRepository.existsByCarsIdAndReviewerId(anyLong(), anyString())).thenReturn(false);
        when(feedbackRepository.countByReviewerIdSince(eq(reviewerId), any(Instant.class))).thenReturn(5L);

        // When & Then
        assertThatThrownBy(() -> feedbackService.createFeedback(createRequest, reviewerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Rate limit exceeded")
                .hasMessageContaining("max 5 feedback submissions per hour");

        verify(feedbackRepository, times(1)).countByReviewerIdSince(eq(reviewerId), any(Instant.class));
        verify(feedbackRepository, never()).save(any(Feedback.class));
    }

    // ========================== GET BY ID ==========================

    @Test
    @DisplayName("getFeedbackById - Should return feedback when found")
    void getFeedbackById_WhenFound_ShouldReturnFeedback() {
        // Given
        Long feedbackId = 1L;
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(testFeedback));
        when(feedbackMapper.toResponse(testFeedback)).thenReturn(testResponse);

        // When
        FeedbackResponse result = feedbackService.getFeedbackById(feedbackId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(feedbackId);

        verify(feedbackRepository, times(1)).findById(feedbackId);
        verify(feedbackMapper, times(1)).toResponse(testFeedback);
    }

    @Test
    @DisplayName("getFeedbackById - Should throw ResourceNotFoundException when not found")
    void getFeedbackById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentFeedbackId = 999L;
        when(feedbackRepository.findById(nonExistentFeedbackId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> feedbackService.getFeedbackById(nonExistentFeedbackId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback not found with id: " + nonExistentFeedbackId);

        verify(feedbackRepository, times(1)).findById(nonExistentFeedbackId);
        verify(feedbackMapper, never()).toResponse(any(Feedback.class));
    }

    // ========================== GET BY CAR ==========================

    @Test
    @DisplayName("getFeedbackByCar - Should return page of feedback")
    void getFeedbackByCar_ShouldReturnPageOfFeedback() {
        // Given
        Long carsId = 100L;
        Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback));
        when(feedbackRepository.findByCarsId(carsId, pageable)).thenReturn(feedbackPage);
        when(feedbackMapper.toResponse(testFeedback)).thenReturn(testResponse);

        // When
        Page<FeedbackResponse> result = feedbackService.getFeedbackByCar(carsId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCarsId()).isEqualTo(carsId);

        verify(feedbackRepository, times(1)).findByCarsId(carsId, pageable);
    }

    // ========================== GET SUMMARY BY CAR ==========================

    @Test
    @DisplayName("getFeedbackSummaryByCar - Should calculate average rating and count")
    void getFeedbackSummaryByCar_ShouldCalculateSummary() {
        // Given
        Long carsId = 100L;
        when(feedbackRepository.calculateAverageRating(carsId)).thenReturn(4.3);
        when(feedbackRepository.countByCarsId(carsId)).thenReturn(15L);

        // When
        CarFeedbackSummary result = feedbackService.getFeedbackSummaryByCar(carsId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCarsId()).isEqualTo(carsId);
        assertThat(result.getAverageRating()).isEqualTo(4.3);
        assertThat(result.getFeedbackCount()).isEqualTo(15L);

        verify(feedbackRepository, times(1)).calculateAverageRating(carsId);
        verify(feedbackRepository, times(1)).countByCarsId(carsId);
    }

    // ========================== GET BY REVIEWER ==========================

    @Test
    @DisplayName("getFeedbackByReviewer - Should return page of feedback")
    void getFeedbackByReviewer_ShouldReturnPageOfFeedback() {
        // Given
        String reviewerId = "auth0|reviewer123";
        Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback));
        when(feedbackRepository.findByReviewerId(reviewerId, pageable)).thenReturn(feedbackPage);
        when(feedbackMapper.toResponse(testFeedback)).thenReturn(testResponse);

        // When
        Page<FeedbackResponse> result = feedbackService.getFeedbackByReviewer(reviewerId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getReviewerId()).isEqualTo(reviewerId);

        verify(feedbackRepository, times(1)).findByReviewerId(reviewerId, pageable);
    }

    // ========================== DELETE FEEDBACK ==========================

    @Test
    @DisplayName("deleteFeedback - Should delete when owner matches")
    void deleteFeedback_WhenOwnerMatches_ShouldDeleteFeedback() {
        // Given
        Long feedbackId = 1L;
        String reviewerId = "auth0|reviewer123";
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(testFeedback));
        doNothing().when(feedbackRepository).delete(testFeedback);

        // When
        feedbackService.deleteFeedback(feedbackId, reviewerId);

        // Then
        verify(feedbackRepository, times(1)).findById(feedbackId);
        verify(feedbackRepository, times(1)).delete(testFeedback);
    }

    @Test
    @DisplayName("deleteFeedback - Should throw ValidationException when owner mismatch")
    void deleteFeedback_WhenOwnerMismatch_ShouldThrowValidationException() {
        // Given
        Long feedbackId = 1L;
        String differentReviewerId = "auth0|otherUser";
        when(feedbackRepository.findById(feedbackId)).thenReturn(Optional.of(testFeedback));

        // When & Then
        assertThatThrownBy(() -> feedbackService.deleteFeedback(feedbackId, differentReviewerId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("You can only delete your own feedback");

        verify(feedbackRepository, times(1)).findById(feedbackId);
        verify(feedbackRepository, never()).delete(any(Feedback.class));
    }

    @Test
    @DisplayName("deleteFeedback - Should throw ResourceNotFoundException when not found")
    void deleteFeedback_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentFeedbackId = 999L;
        String reviewerId = "auth0|reviewer123";
        when(feedbackRepository.findById(nonExistentFeedbackId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> feedbackService.deleteFeedback(nonExistentFeedbackId, reviewerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Feedback not found with id: " + nonExistentFeedbackId);

        verify(feedbackRepository, times(1)).findById(nonExistentFeedbackId);
        verify(feedbackRepository, never()).delete(any(Feedback.class));
    }
}
