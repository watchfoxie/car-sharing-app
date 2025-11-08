package com.services.feedback_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Map;

/**
 * DTO for rating distribution (histogram).
 * 
 * <p>Returned by GET /v1/feedback/cars/{carId}/distribution endpoint.
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Rating distribution for a car or globally")
public class RatingDistribution {

    @Schema(description = "Car ID (null for global distribution)", example = "123", nullable = true)
    private Long carsId;

    @Schema(description = "Map of rating value to count", example = "{\"5.0\": 15, \"4.0\": 8, \"3.0\": 3, \"2.0\": 1, \"1.0\": 0}")
    private Map<Double, Long> distribution;

    @Schema(description = "Total number of feedback entries", example = "27")
    private long totalCount;
}
