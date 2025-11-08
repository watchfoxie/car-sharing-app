package com.services.feedback_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * DTO for car feedback aggregation (average rating + count).
 * 
 * <p>Returned by GET /v1/feedback/cars/{carId}/summary endpoint.
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated feedback summary for a car")
public class CarFeedbackSummary {

    @Schema(description = "Car ID", example = "123")
    private Long carsId;

    @Schema(description = "Average rating (0.0 to 5.0, null if no feedback)", example = "4.32", nullable = true)
    private Double averageRating;

    @Schema(description = "Total number of feedback entries", example = "27")
    private long feedbackCount;
}
