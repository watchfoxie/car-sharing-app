package com.services.rental_service.service;

import com.services.rental_service.aspect.RetryOnConstraintViolation;
import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import com.services.rental_service.domain.repository.RentalRepository;
import com.services.rental_service.dto.*;
import com.services.rental_service.exception.BusinessException;
import com.services.rental_service.exception.InvalidStateTransitionException;
import com.services.rental_service.exception.ResourceNotFoundException;
import com.services.rental_service.exception.ValidationException;
import com.services.rental_service.mapper.RentalMapper;
import com.services.rental_service.sse.RentalStatusSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Service layer for rental business logic.
 * <p>
 * Implements Finite State Machine (FSM) for rental lifecycle:
 * PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED (+ CANCELLED)
 * </p>
 * <p>
 * Key responsibilities:
 * <ul>
 *   <li>Rental creation with conflict-free booking (EXCLUDE constraint)</li>
 *   <li>FSM state transition validation</li>
 *   <li>Idempotency enforcement via (renter_id, idempotency_key)</li>
 *   <li>Cost calculation integration with pricing-rules-service</li>
 *   <li>Owner-based authorization for return approval</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RentalService {

    private static final String RENTAL_NOT_FOUND_PREFIX = "Rental with ID ";
    private static final String RENTAL_NOT_FOUND_SUFFIX = " not found";

    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final RentalStatusSseService sseService;
    // TODO: Inject PricingServiceClient in future phase (Faza 12)

    /**
     * Get rental by ID.
     * <p>
     * Access control: only renter or car owner can view rental details.
     * </p>
     *
     * @param rentalId  the rental ID
     * @param accountId the current user's account ID (from JWT)
     * @return RentalResponse DTO
     * @throws ResourceNotFoundException if rental not found
     * @throws BusinessException         if access denied (not owner/renter)
     */
    @org.springframework.cache.annotation.Cacheable(cacheNames = "rentalDetails", key = "#rentalId")
    public RentalResponse getRentalById(Long rentalId, String accountId) {
        log.debug("Fetching rental by ID: {} for account: {}", rentalId, accountId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(RENTAL_NOT_FOUND_PREFIX + rentalId + RENTAL_NOT_FOUND_SUFFIX));

        // Access control: only renter can view (owner check requires car-service integration, future phase)
        if (!rental.isOwnedBy(accountId)) {
            throw new BusinessException("Access denied: you can only view your own rentals");
        }

        return rentalMapper.toResponse(rental);
    }

    /**
     * Get all rentals for a specific renter.
     * <p>
     * Returns rentals ordered by pickup datetime descending (most recent first).
     * </p>
     *
     * @param renterId the renter's account ID
     * @param pageable pagination parameters
     * @return Page of rentals
     */
    public Page<RentalResponse> getRentalsByRenter(String renterId, Pageable pageable) {
        log.debug("Fetching rentals for renter: {}", renterId);
        return rentalRepository.findByRenterIdOrderByPickupDatetimeDesc(renterId, pageable)
                .map(rentalMapper::toResponse);
    }

    /**
     * Get all rentals for a specific car (operator view).
     * <p>
     * TODO: Validate car ownership in future phase (requires car-service integration).
     * </p>
     *
     * @param carsId   the car ID
     * @param pageable pagination parameters
     * @return Page of rentals
     */
    public Page<RentalResponse> getRentalsByCar(Long carsId, Pageable pageable) {
        log.debug("Fetching rentals for car: {}", carsId);
        return rentalRepository.findByCarsIdOrderByPickupDatetimeDesc(carsId, pageable)
                .map(rentalMapper::toResponse);
    }

    /**
     * Create a new rental (booking).
     * <p>
     * Workflow:
     * <ol>
     *   <li>Check idempotency key (return existing if duplicate)</li>
     *   <li>Validate time period (pickup before return, future dates)</li>
     *   <li>Check car availability (no overlapping active rentals)</li>
     *   <li>Calculate estimated cost (pricing-rules-service integration TODO)</li>
     *   <li>Create rental with status CONFIRMED</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Concurrency:</strong> EXCLUDE constraint in DB prevents race conditions.
     * Retries are handled by {@link RetryAspect} with exponential backoff (max 3 attempts).
     * </p>
     *
     * @param request   CreateRentalRequest DTO
     * @param renterId  the renter's account ID (from JWT)
     * @return RentalResponse DTO (status: CONFIRMED)
     * @throws ValidationException if validation fails
     * @throws BusinessException   if car is not available
     */
    @Transactional
    @RetryOnConstraintViolation(maxAttempts = 3, initialBackoffMs = 100, backoffMultiplier = 2.0)
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "activeRentals", allEntries = true)
    public RentalResponse createRental(CreateRentalRequest request, String renterId) {
        log.info("Creating rental for renter: {}, car: {}, period: {} to {}",
                renterId, request.getCarsId(), request.getPickupDatetime(), request.getReturnDatetime());

        // 1. Check idempotency key
        if (request.getIdempotencyKey() != null) {
            Optional<Rental> existing = rentalRepository.findByRenterIdAndIdempotencyKey(
                    renterId, request.getIdempotencyKey());
            if (existing.isPresent()) {
                log.warn("Duplicate rental request detected (idempotency key: {}). Returning existing rental.",
                        request.getIdempotencyKey());
                return rentalMapper.toResponse(existing.get());
            }
        }

        // 2. Validate time period
        validateRentalPeriod(request.getPickupDatetime(), request.getReturnDatetime());

        // 3. Check car availability (EXCLUDE constraint will prevent conflicts at DB level)
        long overlappingCount = rentalRepository.countOverlappingActiveRentals(
                request.getCarsId(),
                request.getPickupDatetime(),
                request.getReturnDatetime()
        );
        if (overlappingCount > 0) {
            throw new BusinessException(String.format(
                    "Car %d is not available for the requested period (%s to %s). %d overlapping rental(s) found.",
                    request.getCarsId(), request.getPickupDatetime(), request.getReturnDatetime(), overlappingCount
            ));
        }

        // 4. Calculate estimated cost (TODO: integrate with pricing-rules-service)
        BigDecimal estimatedCost = calculateEstimatedCost(request);

        // 5. Create rental
        Rental rental = rentalMapper.toEntity(request);
        rental.setRenterId(renterId);
        rental.setStatus(RentalStatus.CONFIRMED);
        rental.setEstimatedCost(estimatedCost);

        Rental savedRental = rentalRepository.save(rental);
        log.info("Rental created successfully with ID: {}", savedRental.getId());

        // Broadcast SSE event
        String eventData = String.format(
            "{\"rentalId\": %d, \"renterId\": \"%s\", \"carsId\": %d, \"status\": \"%s\", \"timestamp\": \"%s\"}",
            savedRental.getId(), savedRental.getRenterId(), savedRental.getCarsId(), 
            savedRental.getStatus(), Instant.now());
        sseService.broadcastStatusUpdate("rental-confirmed", eventData);

        return rentalMapper.toResponse(savedRental);
    }

    /**
     * Update rental to PICKED_UP status (customer takes possession).
     * <p>
     * FSM transition: CONFIRMED → PICKED_UP
     * </p>
     *
     * @param rentalId  the rental ID
     * @param request   UpdatePickupRequest DTO
     * @param renterId  the renter's account ID (from JWT)
     * @return RentalResponse DTO (status: PICKED_UP)
     * @throws ResourceNotFoundException         if rental not found
     * @throws InvalidStateTransitionException if rental not in CONFIRMED state
     * @throws BusinessException                if access denied
     */
    @Transactional
    public RentalResponse updatePickup(Long rentalId, UpdatePickupRequest request, String renterId) {
        log.info("Updating pickup for rental: {}, renter: {}", rentalId, renterId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(RENTAL_NOT_FOUND_PREFIX + rentalId + RENTAL_NOT_FOUND_SUFFIX));

        // Access control: only renter can pickup
        if (!rental.isOwnedBy(renterId)) {
            throw new BusinessException("Access denied: you can only pickup your own rentals");
        }

        // FSM validation
        if (!rental.canPickup()) {
            throw new InvalidStateTransitionException(String.format(
                    "Cannot pickup rental %d: current status is %s, expected CONFIRMED",
                    rentalId, rental.getStatus()
            ));
        }

        // Update pickup details
        Instant actualPickupTime = request.getActualPickupDatetime() != null
                ? request.getActualPickupDatetime()
                : Instant.now();
        rental.setPickupDatetime(actualPickupTime);

        if (request.getActualPickupLocation() != null) {
            rental.setPickupLocation(request.getActualPickupLocation());
        }

        rental.setStatus(RentalStatus.PICKED_UP);

        Rental updatedRental = rentalRepository.save(rental);
        log.info("Rental {} picked up successfully at {}", rentalId, actualPickupTime);

        return rentalMapper.toResponse(updatedRental);
    }

    /**
     * Update rental to RETURNED status (customer returns vehicle).
     * <p>
     * FSM transition: PICKED_UP → RETURNED
     * </p>
     *
     * @param rentalId  the rental ID
     * @param request   UpdateReturnRequest DTO
     * @param renterId  the renter's account ID (from JWT)
     * @return RentalResponse DTO (status: RETURNED)
     * @throws ResourceNotFoundException         if rental not found
     * @throws InvalidStateTransitionException if rental not in PICKED_UP state
     * @throws BusinessException                if access denied
     */
    @Transactional
    public RentalResponse updateReturn(Long rentalId, UpdateReturnRequest request, String renterId) {
        log.info("Updating return for rental: {}, renter: {}", rentalId, renterId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(RENTAL_NOT_FOUND_PREFIX + rentalId + RENTAL_NOT_FOUND_SUFFIX));

        // Access control: only renter can return
        if (!rental.isOwnedBy(renterId)) {
            throw new BusinessException("Access denied: you can only return your own rentals");
        }

        // FSM validation
        if (!rental.canReturn()) {
            throw new InvalidStateTransitionException(String.format(
                    "Cannot return rental %d: current status is %s, expected PICKED_UP",
                    rentalId, rental.getStatus()
            ));
        }

        // Update return details
        Instant actualReturnTime = request.getActualReturnDatetime() != null
                ? request.getActualReturnDatetime()
                : Instant.now();

        // Validate return is after pickup
        if (actualReturnTime.isBefore(rental.getPickupDatetime())) {
            throw new ValidationException("Return datetime cannot be before pickup datetime");
        }

        rental.setReturnDatetime(actualReturnTime);

        if (request.getActualReturnLocation() != null) {
            rental.setReturnLocation(request.getActualReturnLocation());
        }

        rental.setStatus(RentalStatus.RETURNED);

        Rental updatedRental = rentalRepository.save(rental);
        log.info("Rental {} returned successfully at {}", rentalId, actualReturnTime);

        return rentalMapper.toResponse(updatedRental);
    }

    /**
     * Approve return and finalize rental (operator workflow).
     * <p>
     * FSM transition: RETURNED → RETURN_APPROVED
     * <p>
     * Calculates final cost including penalties for late returns, damages, etc.
     * </p>
     *
     * @param rentalId  the rental ID
     * @param request   ApproveReturnRequest DTO
     * @param operatorId the operator's account ID (from JWT)
     * @return RentalResponse DTO (status: RETURN_APPROVED, finalCost set)
     * @throws ResourceNotFoundException         if rental not found
     * @throws InvalidStateTransitionException if rental not in RETURNED state
     * @throws BusinessException                if access denied (not car owner)
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"activeRentals", "rentalDetails"}, allEntries = true)
    public RentalResponse approveReturn(Long rentalId, ApproveReturnRequest request, String operatorId) {
        log.info("Approving return for rental: {}, operator: {}", rentalId, operatorId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(RENTAL_NOT_FOUND_PREFIX + rentalId + RENTAL_NOT_FOUND_SUFFIX));

        // TODO: Validate operator is car owner (requires car-service integration, future phase)

        // FSM validation
        if (!rental.canApproveReturn()) {
            throw new InvalidStateTransitionException(String.format(
                    "Cannot approve return for rental %d: current status is %s, expected RETURNED",
                    rentalId, rental.getStatus()
            ));
        }

        // Calculate final cost (TODO: integrate with pricing-rules-service for penalties)
        BigDecimal finalCost = calculateFinalCost(rental, request);
        rental.setFinalCost(finalCost);

        rental.setStatus(RentalStatus.RETURN_APPROVED);

        Rental updatedRental = rentalRepository.save(rental);
        log.info("Return approved for rental {}, final cost: {}", rentalId, finalCost);

        return rentalMapper.toResponse(updatedRental);
    }

    /**
     * Cancel rental (only allowed before PICKED_UP).
     * <p>
     * FSM transition: PENDING/CONFIRMED → CANCELLED
     * </p>
     *
     * @param rentalId  the rental ID
     * @param renterId  the renter's account ID (from JWT)
     * @return RentalResponse DTO (status: CANCELLED)
     * @throws ResourceNotFoundException         if rental not found
     * @throws InvalidStateTransitionException if rental cannot be cancelled
     * @throws BusinessException                if access denied
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = {"activeRentals", "rentalDetails"}, key = "#rentalId")
    public RentalResponse cancelRental(Long rentalId, String renterId) {
        log.info("Cancelling rental: {}, renter: {}", rentalId, renterId);

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException(RENTAL_NOT_FOUND_PREFIX + rentalId + RENTAL_NOT_FOUND_SUFFIX));

        // Access control: only renter can cancel
        if (!rental.isOwnedBy(renterId)) {
            throw new BusinessException("Access denied: you can only cancel your own rentals");
        }

        // FSM validation
        if (!rental.isCancellable()) {
            throw new InvalidStateTransitionException(String.format(
                    "Cannot cancel rental %d: current status is %s. Cancellation only allowed for PENDING or CONFIRMED rentals.",
                    rentalId, rental.getStatus()
            ));
        }

        // TODO: Check cancellation_window from pricing-rules-service (future phase)

        rental.setStatus(RentalStatus.CANCELLED);

        Rental updatedRental = rentalRepository.save(rental);
        log.info("Rental {} cancelled successfully", rentalId);

        return rentalMapper.toResponse(updatedRental);
    }

    // ========== Private Helper Methods ==========

    /**
     * Validate rental period constraints.
     *
     * @param pickupDatetime the pickup datetime
     * @param returnDatetime the return datetime
     * @throws ValidationException if validation fails
     */
    private void validateRentalPeriod(Instant pickupDatetime, Instant returnDatetime) {
        if (pickupDatetime.isAfter(returnDatetime)) {
            throw new ValidationException("Pickup datetime must be before return datetime");
        }

        // Note: @Future validation on pickupDatetime is handled by Bean Validation
        // Additional business rules (min/max duration) would be validated here with pricing-rules-service
    }

    /**
     * Calculate estimated cost for rental.
     * <p>
     * TODO: Integrate with pricing-rules-service (POST /v1/pricing/calculate).
     * For now, returns placeholder value.
     * </p>
     *
     * @param request CreateRentalRequest DTO
     * @return estimated cost
     */
    private BigDecimal calculateEstimatedCost(CreateRentalRequest request) {
        // TODO: Call pricing-rules-service API
        // Example: pricingServiceClient.calculatePrice(new CalculatePriceRequest(...))
        log.warn("Estimated cost calculation not yet implemented (pricing-service integration pending)");
        return BigDecimal.valueOf(100.00); // Placeholder
    }

    /**
     * Calculate final cost after return approval.
     * <p>
     * TODO: Integrate with pricing-rules-service for:
     * <ul>
     *   <li>Base cost recalculation (actual vs. estimated period)</li>
     *   <li>Late return penalties</li>
     *   <li>Additional charges from operator</li>
     * </ul>
     * </p>
     *
     * @param rental  the rental entity
     * @param request ApproveReturnRequest DTO
     * @return final cost
     */
    private BigDecimal calculateFinalCost(Rental rental, ApproveReturnRequest request) {
        // TODO: Call pricing-rules-service API with actual period
        log.warn("Final cost calculation not yet implemented (pricing-service integration pending)");

        BigDecimal baseCost = rental.getEstimatedCost() != null
                ? rental.getEstimatedCost()
                : BigDecimal.valueOf(100.00);

        BigDecimal additionalCharges = request.getAdditionalCharges() != null
                ? request.getAdditionalCharges()
                : BigDecimal.ZERO;

        return baseCost.add(additionalCharges);
    }
}
