package com.services.feedback_service.service;

import com.services.feedback_service.domain.repository.FeedbackRepository;
import com.services.feedback_service.dto.RatingDistribution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReportsService}.
 * 
 * <p>Tests feedback analytics and reporting functionality
 * using Mockito to isolate service layer from repository dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>Top cars by rating with minimum feedback threshold</li>
 *   <li>Rating distribution (histogram) for specific car and globally</li>
 *   <li>Aggregation queries with business intelligence filters</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportsService Unit Tests")
class ReportsServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private ReportsService reportsService;

    // ========================== GET TOP CARS BY RATING ==========================

    @Test
    @DisplayName("getTopCarsByRating - Should return top cars with minimum feedback count")
    void getTopCarsByRating_ShouldReturnTopCars() {
        // Given: Mock repository response with [carsId, avgRating, feedbackCount]
        List<Object[]> mockResults = List.of(
                new Object[]{101L, 4.8, 25L},
                new Object[]{102L, 4.5, 18L},
                new Object[]{103L, 4.3, 12L}
        );

        when(feedbackRepository.findTopCarsByRating(anyLong(), any(PageRequest.class)))
                .thenReturn(mockResults);

        // When
        List<ReportsService.TopCarRating> result = reportsService.getTopCarsByRating(3, 10L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        // Verify first car (highest rating)
        assertThat(result.get(0).carsId()).isEqualTo(101L);
        assertThat(result.get(0).averageRating()).isEqualTo(4.8);
        assertThat(result.get(0).feedbackCount()).isEqualTo(25L);

        // Verify second car
        assertThat(result.get(1).carsId()).isEqualTo(102L);
        assertThat(result.get(1).averageRating()).isEqualTo(4.5);
        assertThat(result.get(1).feedbackCount()).isEqualTo(18L);

        verify(feedbackRepository, times(1)).findTopCarsByRating(10L, PageRequest.of(0, 3));
    }

    @Test
    @DisplayName("getTopCarsByRating - Should handle empty results")
    void getTopCarsByRating_WhenNoCarsAvailable_ShouldReturnEmptyList() {
        // Given
        when(feedbackRepository.findTopCarsByRating(anyLong(), any(PageRequest.class)))
                .thenReturn(List.of());

        // When
        List<ReportsService.TopCarRating> result = reportsService.getTopCarsByRating(10, 5L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(feedbackRepository, times(1)).findTopCarsByRating(5L, PageRequest.of(0, 10));
    }

    // ========================== GET RATING DISTRIBUTION ==========================

    @Test
    @DisplayName("getRatingDistribution - Should calculate distribution for specific car")
    void getRatingDistribution_ForSpecificCar_ShouldCalculateDistribution() {
        // Given: Mock rating distribution [rating, count]
        Long carsId = 100L;
        List<Object[]> mockResults = List.of(
                new Object[]{5.0, 10L},
                new Object[]{4.0, 15L},
                new Object[]{3.0, 5L},
                new Object[]{2.0, 2L},
                new Object[]{1.0, 1L}
        );

        when(feedbackRepository.countByRatingDistribution(carsId)).thenReturn(mockResults);

        // When
        RatingDistribution result = reportsService.getRatingDistribution(carsId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCarsId()).isEqualTo(carsId);
        assertThat(result.getTotalCount()).isEqualTo(33L); // 10 + 15 + 5 + 2 + 1

        Map<Double, Long> distribution = result.getDistribution();
        assertThat(distribution).hasSize(5);
        assertThat(distribution.get(5.0)).isEqualTo(10L);
        assertThat(distribution.get(4.0)).isEqualTo(15L);
        assertThat(distribution.get(3.0)).isEqualTo(5L);
        assertThat(distribution.get(2.0)).isEqualTo(2L);
        assertThat(distribution.get(1.0)).isEqualTo(1L);

        verify(feedbackRepository, times(1)).countByRatingDistribution(carsId);
    }

    @Test
    @DisplayName("getRatingDistribution - Should calculate global distribution when carsId is null")
    void getRatingDistribution_ForGlobal_ShouldCalculateDistribution() {
        // Given: Mock global rating distribution
        List<Object[]> mockResults = List.of(
                new Object[]{5.0, 50L},
                new Object[]{4.0, 120L},
                new Object[]{3.0, 30L}
        );

        when(feedbackRepository.countByRatingDistribution(null)).thenReturn(mockResults);

        // When
        RatingDistribution result = reportsService.getRatingDistribution(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCarsId()).isNull();
        assertThat(result.getTotalCount()).isEqualTo(200L); // 50 + 120 + 30

        Map<Double, Long> distribution = result.getDistribution();
        assertThat(distribution).hasSize(3);
        assertThat(distribution.get(5.0)).isEqualTo(50L);
        assertThat(distribution.get(4.0)).isEqualTo(120L);
        assertThat(distribution.get(3.0)).isEqualTo(30L);

        verify(feedbackRepository, times(1)).countByRatingDistribution(null);
    }

    @Test
    @DisplayName("getRatingDistribution - Should handle empty distribution")
    void getRatingDistribution_WhenNoData_ShouldReturnEmptyDistribution() {
        // Given
        Long carsId = 200L;
        when(feedbackRepository.countByRatingDistribution(carsId)).thenReturn(List.of());

        // When
        RatingDistribution result = reportsService.getRatingDistribution(carsId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCarsId()).isEqualTo(carsId);
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getDistribution()).isEmpty();

        verify(feedbackRepository, times(1)).countByRatingDistribution(carsId);
    }
}
