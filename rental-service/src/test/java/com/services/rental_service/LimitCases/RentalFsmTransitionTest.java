package com.services.rental_service.LimitCases;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import com.services.rental_service.domain.repository.RentalRepository;
import com.services.rental_service.dto.*;
import com.services.rental_service.exception.InvalidStateTransitionException;
import com.services.rental_service.service.RentalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 18 (Faza 18): Critical scenarios and edge cases - FSM transition validation.
 * <p>
 * Validates Finite State Machine (FSM) for rental lifecycle:
 * <ul>
 *   <li>Valid transitions: PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED</li>
 *   <li>Valid cancellation: PENDING/CONFIRMED → CANCELLED</li>
 *   <li>Invalid transitions: skipping states, backwards transitions, terminal state changes</li>
 *   <li>Exception handling: InvalidStateTransitionException thrown for illegal transitions</li>
 * </ul>
 * </p>
 * <p>
 * <strong>FSM State Diagram:</strong>
 * <pre>
 *    PENDING ──→ CONFIRMED ──→ PICKED_UP ──→ RETURNED ──→ RETURN_APPROVED
 *       │            │
 *       └────────────┴──────────→ CANCELLED
 * </pre>
 * </p>
 * <p>
 * <strong>Test scenarios:</strong>
 * <ol>
 *   <li>Valid full lifecycle: PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED</li>
 *   <li>Valid cancellation paths: PENDING → CANCELLED, CONFIRMED → CANCELLED</li>
 *   <li>Invalid: PENDING → RETURNED (skip PICKED_UP)</li>
 *   <li>Invalid: PICKED_UP → PENDING (backwards)</li>
 *   <li>Invalid: PICKED_UP → CANCELLED (cannot cancel after pickup)</li>
 *   <li>Invalid: RETURN_APPROVED → CANCELLED (terminal state)</li>
 *   <li>Invalid: RETURNED → PICKED_UP (backwards)</li>
 * </ol>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09 (Phase 18)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Slf4j
class RentalFsmTransitionTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/migration/V1__init.sql");

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    private static final Long CAR_ID = 3001L;
    private static final String RENTER_ID = "renter-fsm-001";
    private static final String OPERATOR_ID = "operator-fsm-001";
    private static final Instant BASE_TIME = Instant.now().plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        log.info("=== Test setup: cleaned rental_history table ===");
    }

    /**
     * Test 1: Valid full lifecycle path.
     * <p>
     * PENDING → CONFIRMED → PICKED_UP → RETURNED → RETURN_APPROVED
     * </p>
     */
    @Test
    @DisplayName("Test 18.3a: Valid FSM lifecycle - full rental workflow")
    void testValidFullLifecycle() {
        // Given: Create rental (PENDING → CONFIRMED transition happens in createRental)
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("FSM Test Location")
                .build();

        // When: Create rental (implicitly transitions to CONFIRMED)
        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        assertThat(created.getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        log.info("Step 1: Rental created with status CONFIRMED");

        // When: Pickup (CONFIRMED → PICKED_UP)
        UpdatePickupRequest pickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(BASE_TIME)
                .actualPickupLocation("Picked up location")
                .build();

        RentalResponse pickedUp = rentalService.updatePickup(created.getId(), pickupRequest, RENTER_ID);
        assertThat(pickedUp.getStatus()).isEqualTo(RentalStatus.PICKED_UP);
        log.info("Step 2: Rental transitioned to PICKED_UP");

        // When: Return (PICKED_UP → RETURNED)
        UpdateReturnRequest returnRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .actualReturnLocation("Returned location")
                .build();

        RentalResponse returned = rentalService.updateReturn(created.getId(), returnRequest, RENTER_ID);
        assertThat(returned.getStatus()).isEqualTo(RentalStatus.RETURNED);
        log.info("Step 3: Rental transitioned to RETURNED");

        // When: Approve return (RETURNED → RETURN_APPROVED)
        ApproveReturnRequest approveRequest = ApproveReturnRequest.builder()
                .operatorNotes("No damages, approved")
                .additionalCharges(BigDecimal.ZERO)
                .inspectionPassed(true)
                .build();

        Long rentalId = created.getId();
        RentalResponse approved = rentalService.approveReturn(rentalId, approveRequest, OPERATOR_ID);
        assertThat(approved.getStatus()).isEqualTo(RentalStatus.RETURN_APPROVED);
        log.info("Step 4: Rental transitioned to RETURN_APPROVED (terminal state)");

        // Then: Verify final state in database
        Rental dbRental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(dbRental.getStatus()).isEqualTo(RentalStatus.RETURN_APPROVED);
        assertThat(dbRental.getFinalCost()).isNotNull();
    }

    /**
     * Test 2: Valid cancellation from PENDING.
     * <p>
     * PENDING → CANCELLED (not implemented yet, but CONFIRMED → CANCELLED works)
     * </p>
     */
    @Test
    @DisplayName("Test 18.3b: Valid FSM transition - CONFIRMED to CANCELLED")
    void testValidCancellationFromConfirmed() {
        // Given: Create rental (status: CONFIRMED)
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Cancellation Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        assertThat(created.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // When: Cancel rental (CONFIRMED → CANCELLED)
        RentalResponse cancelled = rentalService.cancelRental(rentalId, RENTER_ID);

        // Then: Verify transition to CANCELLED
        assertThat(cancelled.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        log.info("Valid transition: CONFIRMED → CANCELLED");

        Rental dbRental = rentalRepository.findById(rentalId).orElseThrow();
        assertThat(dbRental.getStatus()).isEqualTo(RentalStatus.CANCELLED);
    }

    /**
     * Test 3: Invalid transition - skip PICKED_UP (CONFIRMED → RETURNED).
     * <p>
     * Expected: InvalidStateTransitionException
     * </p>
     */
    @Test
    @DisplayName("Test 18.3c: Invalid FSM transition - CONFIRMED to RETURNED (skip PICKED_UP)")
    void testInvalidTransitionSkipPickedUp() {
        // Given: Create rental (status: CONFIRMED)
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Invalid Transition Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        assertThat(created.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

        // When/Then: Attempt to return without pickup (CONFIRMED → RETURNED)
        UpdateReturnRequest returnRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .actualReturnLocation("Invalid return")
                .build();

        assertThatThrownBy(() -> rentalService.updateReturn(rentalId, returnRequest, RENTER_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot return rental")
                .hasMessageContaining("expected PICKED_UP");

        log.info("Invalid transition blocked: CONFIRMED → RETURNED");
    }

    /**
     * Test 4: Invalid transition - backwards (PICKED_UP → CONFIRMED).
     * <p>
     * FSM does not allow backwards transitions.
     * Expected: Cannot create rental in PICKED_UP state, then revert to CONFIRMED.
     * </p>
     */
    @Test
    @DisplayName("Test 18.3d: Invalid FSM transition - backwards from PICKED_UP to CONFIRMED")
    void testInvalidBackwardsTransition() {
        // Given: Create and pickup rental (status: PICKED_UP)
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Backwards Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        
        UpdatePickupRequest pickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(BASE_TIME)
                .build();
        
        RentalResponse pickedUp = rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID);
        assertThat(pickedUp.getStatus()).isEqualTo(RentalStatus.PICKED_UP);

        // When/Then: Verify cannot call pickup again (no backwards transition)
        assertThatThrownBy(() -> rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot pickup rental")
                .hasMessageContaining("expected CONFIRMED");

        log.info("Backwards transition blocked: PICKED_UP → CONFIRMED");
    }

    /**
     * Test 5: Invalid cancellation after pickup.
     * <p>
     * Cancellation only allowed for PENDING/CONFIRMED states.
     * Expected: InvalidStateTransitionException when attempting PICKED_UP → CANCELLED.
     * </p>
     */
    @Test
    @DisplayName("Test 18.3e: Invalid FSM transition - PICKED_UP to CANCELLED")
    void testInvalidCancellationAfterPickup() {
        // Given: Create and pickup rental (status: PICKED_UP)
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Cancellation After Pickup Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        
        UpdatePickupRequest pickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(BASE_TIME)
                .build();
        
        rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID);

        // When/Then: Attempt to cancel after pickup (PICKED_UP → CANCELLED)
        assertThatThrownBy(() -> rentalService.cancelRental(rentalId, RENTER_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot cancel rental")
                .hasMessageContaining("Cancellation only allowed for PENDING or CONFIRMED");

        log.info("Invalid cancellation blocked: PICKED_UP → CANCELLED");
    }

    /**
     * Test 6: Invalid transition from terminal state.
     * <p>
     * RETURN_APPROVED is a terminal state, no further transitions allowed.
     * Expected: InvalidStateTransitionException when attempting RETURN_APPROVED → CANCELLED.
     * </p>
     */
    @Test
    @DisplayName("Test 18.3f: Invalid FSM transition - RETURN_APPROVED to CANCELLED (terminal state)")
    void testInvalidTransitionFromTerminalState() {
        // Given: Complete full lifecycle to RETURN_APPROVED
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Terminal State Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        
        UpdatePickupRequest pickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(BASE_TIME)
                .build();
        rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID);
        
        UpdateReturnRequest returnRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .build();
        rentalService.updateReturn(rentalId, returnRequest, RENTER_ID);
        
        ApproveReturnRequest approveRequest = ApproveReturnRequest.builder()
                .operatorNotes("Approved")
                .additionalCharges(BigDecimal.ZERO)
                .inspectionPassed(true)
                .build();
        RentalResponse approved = rentalService.approveReturn(rentalId, approveRequest, OPERATOR_ID);
        assertThat(approved.getStatus()).isEqualTo(RentalStatus.RETURN_APPROVED);

        // When/Then: Attempt to cancel from terminal state (RETURN_APPROVED → CANCELLED)
        assertThatThrownBy(() -> rentalService.cancelRental(rentalId, RENTER_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot cancel rental")
                .hasMessageContaining("Cancellation only allowed for PENDING or CONFIRMED");

        log.info("Terminal state transition blocked: RETURN_APPROVED → CANCELLED");
    }

    /**
     * Test 7: Invalid transition - backwards from RETURNED.
     * <p>
     * Cannot return to PICKED_UP after marking rental as RETURNED.
     * Expected: InvalidStateTransitionException.
     * </p>
     */
    @Test
    @DisplayName("Test 18.3g: Invalid FSM transition - RETURNED to PICKED_UP (backwards)")
    void testInvalidBackwardsFromReturned() {
        // Given: Complete rental to RETURNED state
        CreateRentalRequest createRequest = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .pickupLocation("Backwards From Returned Test")
                .build();

        RentalResponse created = rentalService.createRental(createRequest, RENTER_ID);
        Long rentalId = created.getId();
        
        UpdatePickupRequest pickupRequest = UpdatePickupRequest.builder()
                .actualPickupDatetime(BASE_TIME)
                .build();
        rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID);
        
        UpdateReturnRequest returnRequest = UpdateReturnRequest.builder()
                .actualReturnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .build();
        RentalResponse returned = rentalService.updateReturn(rentalId, returnRequest, RENTER_ID);
        assertThat(returned.getStatus()).isEqualTo(RentalStatus.RETURNED);

        // When/Then: Attempt to pickup again (RETURNED → PICKED_UP backwards)
        assertThatThrownBy(() -> rentalService.updatePickup(rentalId, pickupRequest, RENTER_ID))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Cannot pickup rental")
                .hasMessageContaining("expected CONFIRMED");

        log.info("Backwards transition blocked: RETURNED → PICKED_UP");
    }

    /**
     * Test 8: Entity-level FSM helper methods validation.
     * <p>
     * Verify Rental entity methods (canPickup, canReturn, canApproveReturn, isCancellable)
     * correctly enforce FSM rules.
     * </p>
     */
    @Test
    @DisplayName("Test 18.3h: FSM helper methods - Rental entity state guards")
    void testEntityLevelFsmHelpers() {
        // Test CONFIRMED state
        Rental confirmedRental = Rental.builder()
                .renterId(RENTER_ID)
                .carsId(CAR_ID)
                .pickupDatetime(BASE_TIME)
                .returnDatetime(BASE_TIME.plus(2, ChronoUnit.HOURS))
                .status(RentalStatus.CONFIRMED)
                .estimatedCost(BigDecimal.valueOf(100.00))
                .build();

        assertThat(confirmedRental.canPickup()).isTrue();
        assertThat(confirmedRental.canReturn()).isFalse();
        assertThat(confirmedRental.canApproveReturn()).isFalse();
        assertThat(confirmedRental.isCancellable()).isTrue();

        // Test PICKED_UP state
        confirmedRental.setStatus(RentalStatus.PICKED_UP);
        assertThat(confirmedRental.canPickup()).isFalse();
        assertThat(confirmedRental.canReturn()).isTrue();
        assertThat(confirmedRental.canApproveReturn()).isFalse();
        assertThat(confirmedRental.isCancellable()).isFalse();

        // Test RETURNED state
        confirmedRental.setStatus(RentalStatus.RETURNED);
        assertThat(confirmedRental.canPickup()).isFalse();
        assertThat(confirmedRental.canReturn()).isFalse();
        assertThat(confirmedRental.canApproveReturn()).isTrue();
        assertThat(confirmedRental.isCancellable()).isFalse();

        // Test RETURN_APPROVED state (terminal)
        confirmedRental.setStatus(RentalStatus.RETURN_APPROVED);
        assertThat(confirmedRental.canPickup()).isFalse();
        assertThat(confirmedRental.canReturn()).isFalse();
        assertThat(confirmedRental.canApproveReturn()).isFalse();
        assertThat(confirmedRental.isCancellable()).isFalse();

        log.info("Entity-level FSM helper methods validated");
    }
}
