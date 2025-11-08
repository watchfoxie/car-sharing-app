package com.services.feedback_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * DTO for feedback response (read-only).
 * 
 * <p>Returned by GET endpoints with full audit trail.
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Feedback details with audit trail")
public class FeedbackResponse {

    @Schema(description = "Feedback ID", example = "1")
    private Long id;

    @Schema(description = "Rating (0.0 to 5.0)", example = "4.5", minimum = "0", maximum = "5")
    private Double rating;

    @Schema(description = "Optional comment from reviewer", example = "Excellent car, very clean!", nullable = true)
    private String comment;

    @Schema(description = "Car ID being reviewed", example = "123")
    private Long carsId;

    @Schema(description = "Reviewer account ID (null if account deleted)", example = "keycloak-user-id-123", nullable = true)
    private String reviewerId;

    @Schema(description = "Timestamp when feedback was created (UTC)", example = "2025-11-07T10:15:30Z")
    private Instant createdDate;

    @Schema(description = "Timestamp when feedback was last modified (UTC)", example = "2025-11-07T10:15:30Z", nullable = true)
    private Instant lastModifiedDate;

    @Schema(description = "User ID who created this feedback", example = "keycloak-user-id-123")
    private String createdBy;

    @Schema(description = "User ID who last modified this feedback", example = "keycloak-user-id-123", nullable = true)
    private String lastModifiedBy;
}
