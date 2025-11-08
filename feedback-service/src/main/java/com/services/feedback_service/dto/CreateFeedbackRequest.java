package com.services.feedback_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO for creating feedback (POST request).
 * 
 * <p>Validation rules:
 * <ul>
 *   <li>rating: required, 0.0 to 5.0</li>
 *   <li>comment: optional, max 5000 characters</li>
 *   <li>carsId: required</li>
 *   <li>rentalId: required (to verify rental completion and prevent duplicates)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create feedback for a completed rental")
public class CreateFeedbackRequest {

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", message = "Rating must be at least 0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5")
    @Schema(description = "Rating (0.0 to 5.0)", example = "4.5", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "5")
    private Double rating;

    @Size(max = 5000, message = "Comment must not exceed 5000 characters")
    @Schema(description = "Optional comment from reviewer", example = "Excellent car, very clean!", nullable = true, maxLength = 5000)
    private String comment;

    @NotNull(message = "Car ID is required")
    @Schema(description = "Car ID being reviewed", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long carsId;

    @NotNull(message = "Rental ID is required")
    @Schema(description = "Rental ID (must be in RETURN_APPROVED status)", example = "456", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long rentalId;
}
