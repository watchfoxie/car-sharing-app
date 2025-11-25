package com.usarbcs.rating.controller;

import com.usarbcs.rating.command.RatingCommand;
import com.usarbcs.rating.dto.RatingDto;
import com.usarbcs.rating.payload.RatingSummaryPayload;
import com.usarbcs.rating.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import static com.usarbcs.core.constants.ResourcePath.RATINGS;
import static com.usarbcs.core.constants.ResourcePath.V1;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(V1 + RATINGS)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rating Controller", description = "CRUD and reporting endpoints for driver ratings")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @Operation(
            summary = "Create or update a driver rating",
            description = "Creates a new rating for a driver/customer pair or refreshes the existing record when the pair already exists."
    )
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rating persisted",
                content = @Content(schema = @Schema(implementation = RatingDto.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<RatingDto> create(@Valid @RequestBody RatingCommand command) {
        final RatingDto rating = ratingService.saveRating(command);
        final URI location = fromCurrentRequest().path("/{id}").buildAndExpand(rating.getId()).toUri();
        return ResponseEntity.created(location).body(rating);
    }

    @GetMapping("/{ratingId}")
    @Operation(summary = "Retrieve a rating by id")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rating found",
                content = @Content(schema = @Schema(implementation = RatingDto.class))),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFoundProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<RatingDto> getById(@Parameter(description = "Rating identifier", required = true)
                                             @PathVariable UUID ratingId) {
        return ResponseEntity.ok(ratingService.getRating(ratingId));
    }

    @GetMapping
    @Operation(
            summary = "Search ratings",
            description = "Filters ratings by driver, customer, or both and returns a pageable result set."
    )
    @ApiResponse(responseCode = "200", description = "Paged result returned")
    public ResponseEntity<Page<RatingDto>> search(
            @Parameter(description = "Filter by driver identifier")
            @RequestParam(required = false) String driverId,
            @Parameter(description = "Filter by customer identifier")
            @RequestParam(required = false) String customerId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ratingService.search(driverId, customerId, pageable));
    }

    @GetMapping("/driver/{driverId}/summary")
    @Operation(summary = "Aggregate rating stats for a driver")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary computed",
                content = @Content(schema = @Schema(implementation = RatingSummaryPayload.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<RatingSummaryPayload> driverSummary(@Parameter(description = "Driver identifier", required = true)
                                                             @PathVariable String driverId) {
        return ResponseEntity.ok(ratingService.summarizeDriver(driverId));
    }

    @DeleteMapping("/{ratingId}")
    @Operation(summary = "Delete a rating")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rating removed"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFoundProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<Void> delete(@Parameter(description = "Rating identifier", required = true)
                                       @PathVariable UUID ratingId) {
        ratingService.delete(ratingId);
        return ResponseEntity.noContent().build();
    }
}
