package com.services.rental_service.controller;

import com.services.rental_service.dto.*;
import com.services.rental_service.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for rental management endpoints.
 * <p>
 * Provides CRUD operations and state transition endpoints for the rental lifecycle.
 * All endpoints require JWT authentication.
 * </p>
 * <p>
 * <strong>Base path:</strong> {@code /v1/rentals}
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/v1/rentals")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rental Management", description = "APIs for managing car rentals and lifecycle transitions")
@SecurityRequirement(name = "Bearer JWT")
public class RentalController {

    private final RentalService rentalService;

    /**
     * Get rental by ID.
     * <p>
     * Access control: only the renter or car owner can view rental details.
     * </p>
     *
     * @param id  rental ID
     * @param jwt current user's JWT
     * @return RentalResponse (HTTP 200)
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get rental by ID",
            description = "Retrieve rental details. Only the renter or car owner can access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rental found",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rental not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> getRentalById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String accountId = jwt.getSubject();
        log.debug("GET /v1/rentals/{} by account: {}", id, accountId);
        RentalResponse response = rentalService.getRentalById(id, accountId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all rentals for the current user (renter view).
     *
     * @param jwt      current user's JWT
     * @param pageable pagination parameters
     * @return Page of rentals (HTTP 200)
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('RENTER')")
    @Operation(
            summary = "Get my rentals",
            description = "Retrieve all rentals for the authenticated user, ordered by pickup datetime (most recent first)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<RentalResponse>> getMyRentals(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "pickupDatetime") Pageable pageable
    ) {
        String renterId = jwt.getSubject();
        log.debug("GET /v1/rentals/my by renter: {}", renterId);
        Page<RentalResponse> rentals = rentalService.getRentalsByRenter(renterId, pageable);
        return ResponseEntity.ok(rentals);
    }

    /**
     * Get all rentals for a specific car (operator view).
     * <p>
     * TODO: Add car ownership validation in future phase (requires car-service integration).
     * </p>
     *
     * @param carsId   car ID
     * @param jwt      current user's JWT
     * @param pageable pagination parameters
     * @return Page of rentals (HTTP 200)
     */
    @GetMapping("/car/{carsId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Get rentals for a specific car",
            description = "Retrieve all rentals for a car (operator view). Only car owners can access."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rentals retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class)))
    })
    public ResponseEntity<Page<RentalResponse>> getRentalsByCar(
            @PathVariable Long carsId,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "pickupDatetime") Pageable pageable
    ) {
        log.debug("GET /v1/rentals/car/{} by account: {}", carsId, jwt.getSubject());
        Page<RentalResponse> rentals = rentalService.getRentalsByCar(carsId, pageable);
        return ResponseEntity.ok(rentals);
    }

    /**
     * Create a new rental (booking).
     * <p>
     * FSM: Initial state is CONFIRMED (after validation).
     * Idempotency: include {@code idempotencyKey} to prevent duplicate bookings.
     * </p>
     *
     * @param request CreateRentalRequest DTO
     * @param jwt     current user's JWT
     * @return RentalResponse (HTTP 201)
     */
    @PostMapping
    @PreAuthorize("hasRole('RENTER')")
    @Operation(
            summary = "Create a new rental",
            description = """
                    Book a car for a specific time period. The rental will be created with status CONFIRMED if available.
                    
                    **Idempotency:** Include `idempotencyKey` to prevent duplicate bookings. If a rental with the same
                    (renterId, idempotencyKey) already exists, the existing rental will be returned.
                    
                    **Conflict Prevention:** The EXCLUDE constraint in the database prevents overlapping active rentals
                    on the same car.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rental created successfully",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Car not available or validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> createRental(
            @Valid @RequestBody CreateRentalRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String renterId = jwt.getSubject();
        log.info("POST /v1/rentals by renter: {}, car: {}", renterId, request.getCarsId());
        RentalResponse response = rentalService.createRental(request, renterId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update rental to PICKED_UP status.
     * <p>
     * FSM transition: CONFIRMED → PICKED_UP
     * </p>
     *
     * @param id      rental ID
     * @param request UpdatePickupRequest DTO
     * @param jwt     current user's JWT
     * @return RentalResponse (HTTP 200)
     */
    @PutMapping("/{id}/pickup")
    @PreAuthorize("hasRole('RENTER')")
    @Operation(
            summary = "Mark rental as picked up",
            description = """
                    Update rental status to PICKED_UP when customer takes possession of the vehicle.
                    
                    **FSM:** CONFIRMED → PICKED_UP
                    
                    **Access:** Only the renter can perform this action.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pickup recorded successfully",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rental not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> updatePickup(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePickupRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String renterId = jwt.getSubject();
        log.info("PUT /v1/rentals/{}/pickup by renter: {}", id, renterId);
        RentalResponse response = rentalService.updatePickup(id, request, renterId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update rental to RETURNED status.
     * <p>
     * FSM transition: PICKED_UP → RETURNED
     * </p>
     *
     * @param id      rental ID
     * @param request UpdateReturnRequest DTO
     * @param jwt     current user's JWT
     * @return RentalResponse (HTTP 200)
     */
    @PutMapping("/{id}/return")
    @PreAuthorize("hasRole('RENTER')")
    @Operation(
            summary = "Mark rental as returned",
            description = """
                    Update rental status to RETURNED when customer returns the vehicle.
                    
                    **FSM:** PICKED_UP → RETURNED
                    
                    **Access:** Only the renter can perform this action.
                    
                    **Next step:** Operator must approve return to finalize the rental.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return recorded successfully",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rental not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> updateReturn(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReturnRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String renterId = jwt.getSubject();
        log.info("PUT /v1/rentals/{}/return by renter: {}", id, renterId);
        RentalResponse response = rentalService.updateReturn(id, request, renterId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve return and finalize rental (operator action).
     * <p>
     * FSM transition: RETURNED → RETURN_APPROVED
     * </p>
     *
     * @param id      rental ID
     * @param request ApproveReturnRequest DTO
     * @param jwt     current user's JWT
     * @return RentalResponse (HTTP 200)
     */
    @PutMapping("/{id}/return-approval")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(
            summary = "Approve return (operator)",
            description = """
                    Approve the return after vehicle inspection and calculate final cost.
                    
                    **FSM:** RETURNED → RETURN_APPROVED
                    
                    **Access:** Only the car owner (operator) can perform this action.
                    
                    **Cost Calculation:** Final cost includes base rental + penalties (late return, damages, etc.).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Return approved successfully",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rental not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> approveReturn(
            @PathVariable Long id,
            @Valid @RequestBody ApproveReturnRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String operatorId = jwt.getSubject();
        log.info("PUT /v1/rentals/{}/return-approval by operator: {}", id, operatorId);
        RentalResponse response = rentalService.approveReturn(id, request, operatorId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel rental (only allowed before PICKED_UP).
     * <p>
     * FSM transition: PENDING/CONFIRMED → CANCELLED
     * </p>
     *
     * @param id  rental ID
     * @param jwt current user's JWT
     * @return RentalResponse (HTTP 200)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RENTER')")
    @Operation(
            summary = "Cancel rental",
            description = """
                    Cancel a rental before pickup.
                    
                    **FSM:** PENDING/CONFIRMED → CANCELLED
                    
                    **Access:** Only the renter can cancel their own rentals.
                    
                    **Restriction:** Cancellation is not allowed after pickup (PICKED_UP status).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rental cancelled successfully",
                    content = @Content(schema = @Schema(implementation = RentalResponse.class))),
            @ApiResponse(responseCode = "404", description = "Rental not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Cannot cancel after pickup",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<RentalResponse> cancelRental(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String renterId = jwt.getSubject();
        log.info("DELETE /v1/rentals/{} by renter: {}", id, renterId);
        RentalResponse response = rentalService.cancelRental(id, renterId);
        return ResponseEntity.ok(response);
    }
}
