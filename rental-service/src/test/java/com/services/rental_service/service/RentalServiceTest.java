package com.services.rental_service.service;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import com.services.rental_service.domain.repository.RentalRepository;
import com.services.rental_service.dto.*;
import com.services.rental_service.exception.BusinessException;
import com.services.rental_service.exception.InvalidStateTransitionException;
import com.services.rental_service.exception.ResourceNotFoundException;
import com.services.rental_service.exception.ValidationException;
import com.services.rental_service.mapper.RentalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RentalService}.
 * 
 * <p>Tests the Finite State Machine (FSM) for rental lifecycle and business logic
 * using Mockito to isolate service layer from repository and mapper dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>FSM transitions: PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED</li>
 *   <li>Cancellation logic (only for PENDING/CONFIRMED status)</li>
 *   <li>Overlap validation for concurrent bookings</li>
 *   <li>Idempotency enforcement via idempotency_key</li>
 *   <li>Access control (renter/owner authorization)</li>
 *   <li>Cost calculation (estimated and final)</li>
 *   <li>Edge cases (invalid transitions, negative time periods, late returns)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RentalService Unit Tests")
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private RentalMapper rentalMapper;

    @InjectMocks
    private RentalService rentalService;

    private Rental testRental;
    private RentalResponse testResponse;
    private CreateRentalRequest createRequest;
    private UpdatePickupRequest updatePickupRequest;
    private UpdateReturnRequest updateReturnRequest;
    private ApproveReturnRequest approveReturnRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        Instant pickup = now.plus(1, ChronoUnit.DAYS);
        Instant returnTime = pickup.plus(3, ChronoUnit.DAYS);

        testRental = Rental.builder()
                .id(1L)
                .renterId("auth0|renter123")
                .carsId(10L)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .status(RentalStatus.CONFIRMED)
                .estimatedCost(new BigDecimal("150.00"))
                .idempotencyKey("idem-key-12345")
                .build();

        testResponse = RentalResponse.builder()
                .id(1L)
                .renterId("auth0|renter123")
                .carsId(10L)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .status(RentalStatus.CONFIRMED)
                .estimatedCost(new BigDecimal("150.00"))
                .build();

        createRequest = CreateRentalRequest.builder()
                .carsId(10L)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .idempotencyKey("idem-key-12345")
                .build();

        updatePickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(pickup)
                .actualPickupLocation("Warehouse A")
                .build();

        updateReturnRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(returnTime)
                .actualReturnLocation("Warehouse B")
                .build();

        approveReturnRequest = ApproveReturnRequest.builder()
                .additionalCharges(new BigDecimal("20.00"))
                .build();

        pageable = PageRequest.of(0, 20);
    }

    // ========================== GET BY ID ==========================

    @Test
    @DisplayName("getRentalById - Should return rental when found and renter matches")
    void getRentalById_WhenFoundAndRenterMatches_ShouldReturnRental() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.getRentalById(rentalId, renterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(rentalId);
        assertThat(result.getRenterId()).isEqualTo(renterId);

        verify(rentalRepository, times(1)).findById(rentalId);
        verify(rentalMapper, times(1)).toResponse(testRental);
    }

    @Test
    @DisplayName("getRentalById - Should throw ResourceNotFoundException when not found")
    void getRentalById_WhenNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentRentalId = 999L;
        String renterId = "auth0|renter123";
        when(rentalRepository.findById(nonExistentRentalId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rentalService.getRentalById(nonExistentRentalId, renterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Rental with ID " + nonExistentRentalId + " not found");

        verify(rentalRepository, times(1)).findById(nonExistentRentalId);
        verify(rentalMapper, never()).toResponse(any(Rental.class));
    }

    @Test
    @DisplayName("getRentalById - Should throw BusinessException when access denied")
    void getRentalById_WhenAccessDenied_ShouldThrowBusinessException() {
        // Given
        Long rentalId = 1L;
        String differentUserId = "auth0|otherUser";
        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.getRentalById(rentalId, differentUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");

        verify(rentalRepository, times(1)).findById(rentalId);
        verify(rentalMapper, never()).toResponse(any(Rental.class));
    }

    // ========================== GET BY RENTER ==========================

    @Test
    @DisplayName("getRentalsByRenter - Should return page of rentals")
    void getRentalsByRenter_ShouldReturnPageOfRentals() {
        // Given
        String renterId = "auth0|renter123";
        Page<Rental> rentalPage = new PageImpl<>(List.of(testRental));
        when(rentalRepository.findByRenterIdOrderByPickupDatetimeDesc(renterId, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        Page<RentalResponse> result = rentalService.getRentalsByRenter(renterId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRenterId()).isEqualTo(renterId);

        verify(rentalRepository, times(1)).findByRenterIdOrderByPickupDatetimeDesc(renterId, pageable);
    }

    // ========================== GET BY CAR ==========================

    @Test
    @DisplayName("getRentalsByCar - Should return page of rentals")
    void getRentalsByCar_ShouldReturnPageOfRentals() {
        // Given
        Long carsId = 10L;
        Page<Rental> rentalPage = new PageImpl<>(List.of(testRental));
        when(rentalRepository.findByCarsIdOrderByPickupDatetimeDesc(carsId, pageable)).thenReturn(rentalPage);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        Page<RentalResponse> result = rentalService.getRentalsByCar(carsId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCarsId()).isEqualTo(carsId);

        verify(rentalRepository, times(1)).findByCarsIdOrderByPickupDatetimeDesc(carsId, pageable);
    }

    // ========================== CREATE RENTAL ==========================

    @Test
    @DisplayName("createRental - Should create rental successfully when car is available")
    void createRental_WhenCarAvailable_ShouldCreateRental() {
        // Given
        String renterId = "auth0|renter123";
        when(rentalRepository.findByRenterIdAndIdempotencyKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(rentalRepository.countOverlappingActiveRentals(anyLong(), any(Instant.class), any(Instant.class))).thenReturn(0L);
        when(rentalMapper.toEntity(createRequest)).thenReturn(testRental);
        when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.createRental(createRequest, renterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        verify(rentalRepository, times(1)).findByRenterIdAndIdempotencyKey(renterId, createRequest.getIdempotencyKey());
        verify(rentalRepository, times(1)).countOverlappingActiveRentals(anyLong(), any(Instant.class), any(Instant.class));
        verify(rentalRepository, times(1)).save(any(Rental.class));
    }

    @Test
    @DisplayName("createRental - Should return existing rental when idempotency key matches")
    void createRental_WhenIdempotencyKeyExists_ShouldReturnExistingRental() {
        // Given
        String renterId = "auth0|renter123";
        when(rentalRepository.findByRenterIdAndIdempotencyKey(renterId, createRequest.getIdempotencyKey()))
                .thenReturn(Optional.of(testRental));
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.createRental(createRequest, renterId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testRental.getId());

        verify(rentalRepository, times(1)).findByRenterIdAndIdempotencyKey(renterId, createRequest.getIdempotencyKey());
        verify(rentalRepository, never()).save(any(Rental.class)); // No new save
    }

    @Test
    @DisplayName("createRental - Should throw BusinessException when car has overlapping rentals")
    void createRental_WhenOverlappingRentals_ShouldThrowBusinessException() {
        // Given
        String renterId = "auth0|renter123";
        when(rentalRepository.findByRenterIdAndIdempotencyKey(anyString(), anyString())).thenReturn(Optional.empty());
        when(rentalRepository.countOverlappingActiveRentals(anyLong(), any(Instant.class), any(Instant.class))).thenReturn(2L);

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(createRequest, renterId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Car")
                .hasMessageContaining("is not available");

        verify(rentalRepository, times(1)).countOverlappingActiveRentals(anyLong(), any(Instant.class), any(Instant.class));
        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("createRental - Should throw ValidationException when pickup is after return")
    void createRental_WhenPickupAfterReturn_ShouldThrowValidationException() {
        // Given: Invalid time period (pickup > return)
        String renterId = "auth0|renter123";
        Instant pickup = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant invalidReturn = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateRentalRequest invalidRequest = CreateRentalRequest.builder()
                .carsId(10L)
                .pickupDatetime(pickup)
                .returnDatetime(invalidReturn)
                .build();

        // No mocks needed - validation happens before repository access

        // When & Then
        assertThatThrownBy(() -> rentalService.createRental(invalidRequest, renterId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Pickup datetime must be before return datetime");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    // ========================== UPDATE PICKUP ==========================

    @Test
    @DisplayName("updatePickup - Should update to PICKED_UP when rental is CONFIRMED")
    void updatePickup_WhenConfirmed_ShouldUpdateToPickedUp() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.CONFIRMED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.updatePickup(rentalId, updatePickupRequest, renterId);

        // Then
        assertThat(result).isNotNull();
        verify(rentalRepository, times(1)).save(testRental);
        assertThat(testRental.getStatus()).isEqualTo(RentalStatus.PICKED_UP);
    }

    @Test
    @DisplayName("updatePickup - Should throw InvalidStateTransitionException when not CONFIRMED")
    void updatePickup_WhenNotConfirmed_ShouldThrowInvalidStateTransitionException() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.CANCELLED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.updatePickup(rentalId, updatePickupRequest, renterId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot pickup rental");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    // ========================== UPDATE RETURN ==========================

    @Test
    @DisplayName("updateReturn - Should update to RETURNED when rental is PICKED_UP")
    void updateReturn_WhenPickedUp_ShouldUpdateToReturned() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.PICKED_UP);
        testRental.setPickupDatetime(Instant.now().minus(2, ChronoUnit.DAYS));

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.updateReturn(rentalId, updateReturnRequest, renterId);

        // Then
        assertThat(result).isNotNull();
        verify(rentalRepository, times(1)).save(testRental);
        assertThat(testRental.getStatus()).isEqualTo(RentalStatus.RETURNED);
    }

    @Test
    @DisplayName("updateReturn - Should throw InvalidStateTransitionException when not PICKED_UP")
    void updateReturn_WhenNotPickedUp_ShouldThrowInvalidStateTransitionException() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.CONFIRMED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.updateReturn(rentalId, updateReturnRequest, renterId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot return rental");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("updateReturn - Should throw ValidationException when return is before pickup")
    void updateReturn_WhenReturnBeforePickup_ShouldThrowValidationException() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        Instant pickupTime = Instant.now();
        Instant invalidReturnTime = pickupTime.minus(1, ChronoUnit.HOURS);

        testRental.setStatus(RentalStatus.PICKED_UP);
        testRental.setPickupDatetime(pickupTime);

        UpdateReturnRequest invalidRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(invalidReturnTime)
                .build();

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.updateReturn(rentalId, invalidRequest, renterId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Return datetime cannot be before pickup datetime");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    // ========================== APPROVE RETURN ==========================

    @Test
    @DisplayName("approveReturn - Should finalize rental when RETURNED")
    void approveReturn_WhenReturned_ShouldFinalizeRental() {
        // Given
        Long rentalId = 1L;
        String operatorId = "auth0|operator123";
        testRental.setStatus(RentalStatus.RETURNED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.approveReturn(rentalId, approveReturnRequest, operatorId);

        // Then
        assertThat(result).isNotNull();
        verify(rentalRepository, times(1)).save(testRental);
        assertThat(testRental.getStatus()).isEqualTo(RentalStatus.RETURN_APPROVED);
        assertThat(testRental.getFinalCost()).isNotNull();
    }

    @Test
    @DisplayName("approveReturn - Should throw InvalidStateTransitionException when not RETURNED")
    void approveReturn_WhenNotReturned_ShouldThrowInvalidStateTransitionException() {
        // Given
        Long rentalId = 1L;
        String operatorId = "auth0|operator123";
        testRental.setStatus(RentalStatus.PICKED_UP);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.approveReturn(rentalId, approveReturnRequest, operatorId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot approve return");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    // ========================== CANCEL RENTAL ==========================

    @Test
    @DisplayName("cancelRental - Should cancel when rental is CONFIRMED")
    void cancelRental_WhenConfirmed_ShouldCancelRental() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.CONFIRMED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));
        when(rentalRepository.save(testRental)).thenReturn(testRental);
        when(rentalMapper.toResponse(testRental)).thenReturn(testResponse);

        // When
        RentalResponse result = rentalService.cancelRental(rentalId, renterId);

        // Then
        assertThat(result).isNotNull();
        verify(rentalRepository, times(1)).save(testRental);
        assertThat(testRental.getStatus()).isEqualTo(RentalStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelRental - Should throw InvalidStateTransitionException when rental is PICKED_UP")
    void cancelRental_WhenPickedUp_ShouldThrowInvalidStateTransitionException() {
        // Given
        Long rentalId = 1L;
        String renterId = "auth0|renter123";
        testRental.setStatus(RentalStatus.PICKED_UP);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.cancelRental(rentalId, renterId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot cancel rental");

        verify(rentalRepository, never()).save(any(Rental.class));
    }

    @Test
    @DisplayName("cancelRental - Should throw BusinessException when access denied")
    void cancelRental_WhenAccessDenied_ShouldThrowBusinessException() {
        // Given
        Long rentalId = 1L;
        String differentUserId = "auth0|otherUser";
        testRental.setStatus(RentalStatus.CONFIRMED);

        when(rentalRepository.findById(rentalId)).thenReturn(Optional.of(testRental));

        // When & Then
        assertThatThrownBy(() -> rentalService.cancelRental(rentalId, differentUserId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");

        verify(rentalRepository, never()).save(any(Rental.class));
    }
}
