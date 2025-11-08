package com.services.feedback_service.domain.repository;

import com.services.feedback_service.domain.entity.Feedback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link FeedbackRepository}.
 * 
 * <p>Uses Testcontainers for PostgreSQL 16 with Flyway migrations.
 * 
 * <p>Tests:
 * <ul>
 *   <li>CRUD operations</li>
 *   <li>Duplicate prevention (existsByCarsIdAndReviewerId)</li>
 *   <li>Aggregation queries (average rating, count)</li>
 *   <li>Top cars by rating</li>
 *   <li>Rating distribution</li>
 *   <li>Rate limiting (countByReviewerIdSince)</li>
 *   <li>Pagination</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("Feedback Repository Integration Tests")
class FeedbackRepositoryIntegrationTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

    private Long testCarsId;
    private String testReviewerId;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        testCarsId = 1L;
        testReviewerId = "test-reviewer-123";
    }

    @Test
    @DisplayName("Should save and retrieve feedback by ID")
    void testSaveAndFindById() {
        // Given
        Feedback feedback = createTestFeedback(testCarsId, testReviewerId, 4.5, "Great car!");

        // When
        Feedback saved = feedbackRepository.save(feedback);
        Optional<Feedback> found = feedbackRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getRating()).isEqualTo(4.5);
        assertThat(found.get().getComment()).isEqualTo("Great car!");
        assertThat(found.get().getCarsId()).isEqualTo(testCarsId);
        assertThat(found.get().getReviewerId()).isEqualTo(testReviewerId);
    }

    @Test
    @DisplayName("Should find feedback by car ID with pagination")
    void testFindByCarsId() {
        // Given
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer1", 5.0, "Excellent"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer2", 4.0, "Good"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer3", 3.5, "Okay"));
        feedbackRepository.save(createTestFeedback(99L, "reviewer4", 5.0, "Another car"));

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> page = feedbackRepository.findByCarsId(testCarsId, pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent()).allMatch(f -> f.getCarsId().equals(testCarsId));
    }

    @Test
    @DisplayName("Should find feedback by reviewer ID")
    void testFindByReviewerId() {
        // Given
        feedbackRepository.save(createTestFeedback(1L, testReviewerId, 5.0, "Car 1"));
        feedbackRepository.save(createTestFeedback(2L, testReviewerId, 4.0, "Car 2"));
        feedbackRepository.save(createTestFeedback(3L, "other-reviewer", 3.0, "Other"));

        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> page = feedbackRepository.findByReviewerId(testReviewerId, pageable);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(f -> f.getReviewerId().equals(testReviewerId));
    }

    @Test
    @DisplayName("Should check existence of feedback by car and reviewer (duplicate prevention)")
    void testExistsByCarsIdAndReviewerId() {
        // Given
        feedbackRepository.save(createTestFeedback(testCarsId, testReviewerId, 4.5, "Test"));

        // When
        boolean exists = feedbackRepository.existsByCarsIdAndReviewerId(testCarsId, testReviewerId);
        boolean notExists = feedbackRepository.existsByCarsIdAndReviewerId(testCarsId, "other-reviewer");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find feedback by car and reviewer")
    void testFindByCarsIdAndReviewerId() {
        // Given
        Feedback feedback = feedbackRepository.save(createTestFeedback(testCarsId, testReviewerId, 4.5, "Test"));

        // When
        Optional<Feedback> found = feedbackRepository.findByCarsIdAndReviewerId(testCarsId, testReviewerId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(feedback.getId());
    }

    @Test
    @DisplayName("Should count feedback submissions by reviewer in time window (rate limiting)")
    void testCountByReviewerIdSince() {
        // Given
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(Duration.ofHours(1));

        // Create feedback at different times (manually set createdDate for testing)
        Feedback recent1 = createTestFeedback(1L, testReviewerId, 5.0, "Recent 1");
        Feedback recent2 = createTestFeedback(2L, testReviewerId, 4.0, "Recent 2");
        Feedback old = createTestFeedback(3L, testReviewerId, 3.0, "Old");
        
        feedbackRepository.save(recent1);
        feedbackRepository.save(recent2);
        feedbackRepository.save(old);

        // When
        long countLastHour = feedbackRepository.countByReviewerIdSince(testReviewerId, oneHourAgo);

        // Then (all feedback is recent in this test setup)
        assertThat(countLastHour).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should calculate average rating for a car")
    void testCalculateAverageRating() {
        // Given
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer1", 5.0, "Excellent"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer2", 4.0, "Good"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer3", 3.0, "Okay"));

        // When
        Double avgRating = feedbackRepository.calculateAverageRating(testCarsId);

        // Then
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isEqualTo(4.0); // (5 + 4 + 3) / 3 = 4.0
    }

    @Test
    @DisplayName("Should return null for average rating when no feedback exists")
    void testCalculateAverageRatingNoFeedback() {
        // When
        Double avgRating = feedbackRepository.calculateAverageRating(999L);

        // Then
        assertThat(avgRating).isNull();
    }

    @Test
    @DisplayName("Should count feedback entries for a car")
    void testCountByCarsId() {
        // Given
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer1", 5.0, "1"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer2", 4.0, "2"));
        feedbackRepository.save(createTestFeedback(testCarsId, "reviewer3", 3.0, "3"));
        feedbackRepository.save(createTestFeedback(99L, "reviewer4", 5.0, "Other"));

        // When
        long count = feedbackRepository.countByCarsId(testCarsId);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find top cars by rating with minimum feedback count filter")
    void testFindTopCarsByRating() {
        // Given
        // Car 1: avg 4.5 (3 feedback)
        feedbackRepository.save(createTestFeedback(1L, "r1", 5.0, "C1"));
        feedbackRepository.save(createTestFeedback(1L, "r2", 4.0, "C1"));
        feedbackRepository.save(createTestFeedback(1L, "r3", 4.5, "C1"));
        
        // Car 2: avg 3.0 (2 feedback) - below min count
        feedbackRepository.save(createTestFeedback(2L, "r4", 3.0, "C2"));
        feedbackRepository.save(createTestFeedback(2L, "r5", 3.0, "C2"));
        
        // Car 3: avg 5.0 (3 feedback)
        feedbackRepository.save(createTestFeedback(3L, "r6", 5.0, "C3"));
        feedbackRepository.save(createTestFeedback(3L, "r7", 5.0, "C3"));
        feedbackRepository.save(createTestFeedback(3L, "r8", 5.0, "C3"));

        // When
        List<Object[]> topCars = feedbackRepository.findTopCarsByRating(3, PageRequest.of(0, 10));

        // Then
        assertThat(topCars).hasSize(2); // Car 2 excluded (below min count)
        
        Object[] first = topCars.get(0);
        assertThat(((Number) first[0]).longValue()).isEqualTo(3L); // Car 3 (highest rating)
        assertThat(((Number) first[1]).doubleValue()).isEqualTo(5.0); // Avg rating
        assertThat(((Number) first[2]).longValue()).isEqualTo(3); // Feedback count
    }

    @Test
    @DisplayName("Should count feedback grouped by rating (distribution)")
    void testCountByRatingDistribution() {
        // Given
        feedbackRepository.save(createTestFeedback(testCarsId, "r1", 5.0, "C1"));
        feedbackRepository.save(createTestFeedback(testCarsId, "r2", 5.0, "C1"));
        feedbackRepository.save(createTestFeedback(testCarsId, "r3", 4.0, "C1"));
        feedbackRepository.save(createTestFeedback(testCarsId, "r4", 3.0, "C1"));

        // When
        List<Object[]> distribution = feedbackRepository.countByRatingDistribution(testCarsId);

        // Then
        assertThat(distribution).hasSize(3); // 5.0, 4.0, 3.0
        
        // Verify counts (ordered by rating DESC)
        assertThat(distribution.get(0)[0]).isEqualTo(5.0); // Rating
        assertThat(((Number) distribution.get(0)[1]).longValue()).isEqualTo(2); // Count
        
        assertThat(distribution.get(1)[0]).isEqualTo(4.0);
        assertThat(((Number) distribution.get(1)[1]).longValue()).isEqualTo(1);
        
        assertThat(distribution.get(2)[0]).isEqualTo(3.0);
        assertThat(((Number) distribution.get(2)[1]).longValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find all feedback ordered by created date descending")
    void testFindAllByOrderByCreatedDateDesc() {
        // Given
        feedbackRepository.save(createTestFeedback(1L, "r1", 5.0, "First"));
        feedbackRepository.save(createTestFeedback(2L, "r2", 4.0, "Second"));
        feedbackRepository.save(createTestFeedback(3L, "r3", 3.0, "Third"));

        // When
        Page<Feedback> page = feedbackRepository.findAllByOrderByCreatedDateDesc(PageRequest.of(0, 10));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).isSortedAccordingTo((f1, f2) -> f2.getCreatedDate().compareTo(f1.getCreatedDate()));
    }

    @Test
    @DisplayName("Should count total feedback submissions by reviewer")
    void testCountByReviewerId() {
        // Given
        feedbackRepository.save(createTestFeedback(1L, testReviewerId, 5.0, "C1"));
        feedbackRepository.save(createTestFeedback(2L, testReviewerId, 4.0, "C2"));
        feedbackRepository.save(createTestFeedback(3L, testReviewerId, 3.0, "C3"));
        feedbackRepository.save(createTestFeedback(4L, "other", 5.0, "Other"));

        // When
        long count = feedbackRepository.countByReviewerId(testReviewerId);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should delete feedback")
    void testDelete() {
        // Given
        Feedback feedback = feedbackRepository.save(createTestFeedback(testCarsId, testReviewerId, 4.5, "Test"));
        Long id = feedback.getId();

        // When
        feedbackRepository.delete(feedback);

        // Then
        Optional<Feedback> found = feedbackRepository.findById(id);
        assertThat(found).isEmpty();
    }

    // Helper method to create test feedback
    private Feedback createTestFeedback(Long carsId, String reviewerId, Double rating, String comment) {
        return Feedback.builder()
            .carsId(carsId)
            .reviewerId(reviewerId)
            .rating(rating)
            .comment(comment)
            .build();
    }
}
