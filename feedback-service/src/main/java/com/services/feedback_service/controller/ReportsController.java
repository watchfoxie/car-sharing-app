package com.services.feedback_service.controller;

import com.services.feedback_service.dto.RatingDistribution;
import com.services.feedback_service.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for feedback reports and analytics.
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>GET /v1/feedback/reports/top-cars - Top cars by rating</li>
 *   <li>GET /v1/feedback/reports/distribution - Rating distribution</li>
 * </ul>
 * 
 * <p>Public endpoints (no authentication required).
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/v1/feedback/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feedback Reports", description = "Analytics and reports API")
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * Retrieves top N cars by average rating.
     * 
     * @param limit maximum number of cars to return (default: 10, max: 100)
     * @param minFeedbackCount minimum feedback count for inclusion (default: 3)
     * @return list of top cars with ratings
     */
    @GetMapping("/top-cars")
    @Operation(
        summary = "Get top cars by rating",
        description = "Retrieve top cars ranked by average rating. Public endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Top cars retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ReportsService.TopCarRating> getTopCars(
            @Parameter(description = "Maximum number of cars to return (max: 100)", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "Minimum feedback count for inclusion", example = "3")
            @RequestParam(defaultValue = "3") long minFeedbackCount) {
        
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("Limit must be between 1 and 100");
        }
        
        if (minFeedbackCount < 1) {
            throw new IllegalArgumentException("Minimum feedback count must be at least 1");
        }
        
        log.debug("Retrieving top {} cars (min feedback: {})", limit, minFeedbackCount);
        return reportsService.getTopCarsByRating(limit, minFeedbackCount);
    }

    /**
     * Retrieves rating distribution for a specific car or globally.
     * 
     * @param carsId car ID (optional, null for global distribution)
     * @return rating distribution histogram
     */
    @GetMapping("/distribution")
    @Operation(
        summary = "Get rating distribution",
        description = "Retrieve rating distribution (histogram) for a specific car or globally. Public endpoint."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Distribution retrieved successfully",
            content = @Content(schema = @Schema(implementation = RatingDistribution.class)))
    })
    public RatingDistribution getRatingDistribution(
            @Parameter(description = "Car ID (optional, null for global distribution)", example = "123")
            @RequestParam(required = false) Long carsId) {
        log.debug("Retrieving rating distribution for car: {}", carsId);
        return reportsService.getRatingDistribution(carsId);
    }
}
