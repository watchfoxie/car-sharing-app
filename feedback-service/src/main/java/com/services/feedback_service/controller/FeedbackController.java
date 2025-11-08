package com.services.feedback_service.controller;

import com.services.feedback_service.dto.CarFeedbackSummary;
import com.services.feedback_service.dto.CreateFeedbackRequest;
import com.services.feedback_service.dto.FeedbackResponse;
import com.services.feedback_service.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for feedback operations.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>POST /v1/feedback - Create feedback (authenticated)</li>
 *   <li>GET /v1/feedback/cars/{carsId} - List feedback by car (public)</li>
 *   <li>GET /v1/feedback/cars/{carsId}/summary - Aggregated summary (public)</li>
 *   <li>GET /v1/feedback/my - My submitted feedback (authenticated)</li>
 *   <li>DELETE /v1/feedback/{id} - Delete own feedback (authenticated)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/v1/feedback")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback", description = "Customer feedback API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * Creates new feedback for a completed rental.
     * 
     * @param request feedback request
     * @param jwt JWT token (for reviewer ID extraction)
     * @return created feedback
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create feedback",
        description = "Submit feedback for a completed rental. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Feedback created successfully",
            content = @Content(schema = @Schema(implementation = FeedbackResponse.class))),
        @ApiResponse(responseCode = "400", description = "Rate limit exceeded",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Validation error (duplicate or invalid data)",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public FeedbackResponse createFeedback(
            @Valid @RequestBody CreateFeedbackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String reviewerId = jwt.getSubject();
        log.info("Feedback creation requested by reviewer: {}", reviewerId);
        return feedbackService.createFeedback(request, reviewerId);
    }

    /**
     * Retrieves all feedback for a specific car with pagination.
     * 
     * @param carsId car ID
     * @param pageable pagination parameters
     * @return page of feedback
     */
    @GetMapping("/cars/{carsId}")
    @Operation(
        summary = "List feedback by car",
        description = "Retrieve all feedback for a specific car with pagination. Public endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feedback list retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public Page<FeedbackResponse> getFeedbackByCar(
            @Parameter(description = "Car ID", example = "123")
            @PathVariable Long carsId,
            @PageableDefault(size = 20, sort = "createdDate,desc") Pageable pageable) {
        log.debug("Retrieving feedback for car: {}", carsId);
        return feedbackService.getFeedbackByCar(carsId, pageable);
    }

    /**
     * Retrieves aggregated feedback summary for a specific car.
     * 
     * @param carsId car ID
     * @return feedback summary with average rating and count
     */
    @GetMapping("/cars/{carsId}/summary")
    @Operation(
        summary = "Get feedback summary",
        description = "Retrieve aggregated feedback (average rating, count) for a car. Public endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Summary retrieved successfully",
            content = @Content(schema = @Schema(implementation = CarFeedbackSummary.class)))
    })
    public CarFeedbackSummary getFeedbackSummary(
            @Parameter(description = "Car ID", example = "123")
            @PathVariable Long carsId) {
        log.debug("Retrieving feedback summary for car: {}", carsId);
        return feedbackService.getFeedbackSummaryByCar(carsId);
    }

    /**
     * Retrieves feedback submitted by current user.
     * 
     * @param jwt JWT token (for reviewer ID extraction)
     * @param pageable pagination parameters
     * @return page of feedback
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get my feedback",
        description = "Retrieve all feedback submitted by current user. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Feedback list retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Page<FeedbackResponse> getMyFeedback(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "createdDate,desc") Pageable pageable) {
        String reviewerId = jwt.getSubject();
        log.debug("Retrieving feedback by reviewer: {}", reviewerId);
        return feedbackService.getFeedbackByReviewer(reviewerId, pageable);
    }

    /**
     * Deletes own feedback.
     * 
     * @param id feedback ID
     * @param jwt JWT token (for authorization)
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Delete feedback",
        description = "Delete own feedback. Requires authentication."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Feedback deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Feedback not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Cannot delete feedback not owned by you",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void deleteFeedback(
            @Parameter(description = "Feedback ID", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String reviewerId = jwt.getSubject();
        log.info("Feedback deletion requested: id={}, reviewer={}", id, reviewerId);
        feedbackService.deleteFeedback(id, reviewerId);
    }
}
