package com.services.car_service.controller;

import com.services.car_service.domain.enums.VehicleCategory;
import com.services.car_service.dto.CarResponse;
import com.services.car_service.dto.CreateCarRequest;
import com.services.car_service.dto.UpdateCarRequest;
import com.services.car_service.service.CarService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * REST controller for Car management endpoints.
 * 
 * <p>Base path: {@code /v1/cars}
 * 
 * <p>Endpoints:
 * <ul>
 *   <li>GET /v1/cars - List public cars (with filters and sorting)</li>
 *   <li>GET /v1/cars/{id} - Get car by ID</li>
 *   <li>GET /v1/cars/my - List current user's cars</li>
 *   <li>POST /v1/cars - Create a new car</li>
 *   <li>PUT /v1/cars/{id} - Update a car (owner only)</li>
 *   <li>DELETE /v1/cars/{id} - Delete a car (owner only, soft delete)</li>
 * </ul>
 * 
 * <p>Sorting options (via {@code sort} parameter):
 * <ul>
 *   <li>{@code brand,asc} - A-Z by brand</li>
 *   <li>{@code brand,desc} - Z-A by brand</li>
 *   <li>{@code dailyPrice,asc} - Price ascending</li>
 *   <li>{@code dailyPrice,desc} - Price descending</li>
 * </ul>
 * 
 * <p>All endpoints require JWT Bearer authentication except GET public listings.
 * 
 * @see CarService
 * @see CarResponse
 * @see CreateCarRequest
 * @see UpdateCarRequest
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@RestController
@RequestMapping("/v1/cars")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cars", description = "Car fleet management API")
@SecurityRequirement(name = "bearer-jwt")
public class CarController {

    private final CarService carService;

    /**
     * Lists public cars with optional filters and sorting.
     *
     * @param brand optional brand filter (case-insensitive)
     * @param category optional vehicle category filter
     * @param minPrice minimum daily price (default 0)
     * @param maxPrice maximum daily price (default 10000)
     * @param page page number (0-indexed, default 0)
     * @param size page size (default 20, max 100)
     * @param sort sorting criteria (e.g., "brand,asc", "dailyPrice,desc")
     * @return a page of public cars
     */
    @Operation(summary = "List public cars", 
               description = "Retrieves publicly visible cars (shareable and not archived) with optional filtering and sorting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved public cars"),
        @ApiResponse(responseCode = "422", description = "Validation error (e.g., minPrice > maxPrice)",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<Page<CarResponse>> getPublicCars(
            @Parameter(description = "Brand filter (case-insensitive)")
            @RequestParam(required = false) String brand,
            
            @Parameter(description = "Vehicle category filter")
            @RequestParam(required = false) VehicleCategory category,
            
            @Parameter(description = "Minimum daily price (inclusive)")
            @RequestParam(required = false, defaultValue = "0") BigDecimal minPrice,
            
            @Parameter(description = "Maximum daily price (inclusive)")
            @RequestParam(required = false, defaultValue = "10000") BigDecimal maxPrice,
            
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size (max 100)")
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sorting criteria (e.g., 'brand,asc', 'dailyPrice,desc')")
            @RequestParam(defaultValue = "brand,asc") String[] sort) {
        
        log.debug("GET /v1/cars - brand: {}, category: {}, price: {}-{}, page: {}, size: {}", 
                  brand, category, minPrice, maxPrice, page, size);

        Pageable pageable = createPageable(page, size, sort);
        
        Page<CarResponse> cars;
        if (brand != null || category != null) {
            cars = carService.getPublicCarsWithFilters(brand, category, minPrice, maxPrice, pageable);
        } else if (!minPrice.equals(BigDecimal.ZERO) || !maxPrice.equals(BigDecimal.valueOf(10000))) {
            cars = carService.getPublicCarsByPriceRange(minPrice, maxPrice, pageable);
        } else {
            cars = carService.getPublicCars(pageable);
        }
        
        return ResponseEntity.ok(cars);
    }

    /**
     * Retrieves a car by ID.
     *
     * @param id the car ID
     * @return the car response
     */
    @Operation(summary = "Get car by ID", 
               description = "Retrieves detailed information about a specific car")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved car"),
        @ApiResponse(responseCode = "404", description = "Car not found",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CarResponse> getCarById(@PathVariable Long id) {
        log.debug("GET /v1/cars/{}", id);
        CarResponse car = carService.getCarById(id);
        return ResponseEntity.ok(car);
    }

    /**
     * Lists cars owned by the current authenticated user.
     *
     * @param jwt the JWT token (automatically injected)
     * @param page page number (0-indexed, default 0)
     * @param size page size (default 20)
     * @param sort sorting criteria
     * @return a page of owner's cars
     */
    @Operation(summary = "List my cars", 
               description = "Retrieves all cars owned by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved owner's cars"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/my")
    public ResponseEntity<Page<CarResponse>> getMyCars(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdDate,desc") String[] sort) {
        
        String ownerId = jwt.getSubject();
        log.debug("GET /v1/cars/my - owner: {}", ownerId);
        
        Pageable pageable = createPageable(page, size, sort);
        Page<CarResponse> cars = carService.getCarsByOwner(ownerId, pageable);
        
        return ResponseEntity.ok(cars);
    }

    /**
     * Creates a new car.
     *
     * @param request the creation request
     * @param jwt the JWT token (automatically injected)
     * @return the created car response
     */
    @Operation(summary = "Create a new car", 
               description = "Creates a new car in the fleet (owner-based access)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Car created successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Validation error (e.g., duplicate registration number)",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<CarResponse> createCar(
            @Valid @RequestBody CreateCarRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String ownerId = jwt.getSubject();
        log.info("POST /v1/cars - owner: {}, registrationNumber: {}", ownerId, request.getRegistrationNumber());
        
        CarResponse createdCar = carService.createCar(request, ownerId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCar);
    }

    /**
     * Updates an existing car (owner-only access).
     *
     * @param id the car ID
     * @param request the update request
     * @param jwt the JWT token (automatically injected)
     * @return the updated car response
     */
    @Operation(summary = "Update a car", 
               description = "Updates an existing car (only the owner can update)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Car updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Car not found",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "422", description = "Validation error",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CarResponse> updateCar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String requesterId = jwt.getSubject();
        log.info("PUT /v1/cars/{} - requester: {}", id, requesterId);
        
        CarResponse updatedCar = carService.updateCar(id, request, requesterId);
        
        return ResponseEntity.ok(updatedCar);
    }

    /**
     * Deletes a car (owner-only access, soft delete).
     *
     * @param id the car ID
     * @param jwt the JWT token (automatically injected)
     * @return no content
     */
    @Operation(summary = "Delete a car", 
               description = "Soft deletes a car by marking it as archived (only the owner can delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Car deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid token",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden - not the owner",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Car not found",
                     content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        
        String requesterId = jwt.getSubject();
        log.info("DELETE /v1/cars/{} - requester: {}", id, requesterId);
        
        carService.deleteCar(id, requesterId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to create Pageable from request parameters.
     */
    private Pageable createPageable(int page, int size, String[] sort) {
        // Limit page size to 100
        int safeSize = Math.min(size, 100);
        
        // Parse sorting
        Sort sorting = Sort.unsorted();
        if (sort.length > 0 && !sort[0].isEmpty()) {
            String[] sortParams = sort[0].split(",");
            String property = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && "desc".equalsIgnoreCase(sortParams[1])
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            sorting = Sort.by(direction, property);
        }
        
        return PageRequest.of(page, safeSize, sorting);
    }
}
