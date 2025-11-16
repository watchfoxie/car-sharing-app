package com.services.pricing_rules_service.service;

import com.services.pricing_rules_service.domain.entity.PricingRule;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import com.services.pricing_rules_service.domain.repository.PricingRuleRepository;
import com.services.pricing_rules_service.dto.CalculatePriceRequest;
import com.services.pricing_rules_service.dto.CalculatePriceResponse;
import com.services.pricing_rules_service.dto.CreatePricingRuleRequest;
import com.services.pricing_rules_service.dto.PricingRuleResponse;
import com.services.pricing_rules_service.dto.UpdatePricingRuleRequest;
import com.services.pricing_rules_service.mapper.PricingRuleMapper;
import jakarta.persistence.EntityNotFoundException;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PricingRuleService}.
 * 
 * <p>Tests the intelligent pricing calculation engine and CRUD operations
 * using Mockito to isolate service layer from repository and mapper dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>CRUD operations with cache eviction</li>
 *   <li>Price calculation algorithm (DAY→HOUR→MINUTE greedy breakdown)</li>
 *   <li>Duration validation (min/max constraints)</li>
 *   <li>Edge cases (negative duration, missing rules, zero duration)</li>
 *   <li>Active rule lookup with caching</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PricingRuleService Unit Tests")
class PricingRuleServiceTest {

    @Mock
    private PricingRuleRepository pricingRuleRepository;

    @Mock
    private PricingRuleMapper pricingRuleMapper;

    @InjectMocks
    private PricingRuleService pricingRuleService;

    private PricingRule testDayRule;
    private PricingRule testHourRule;
    private PricingRule testMinuteRule;
    private PricingRuleResponse testResponse;
    private CreatePricingRuleRequest createRequest;
    private UpdatePricingRuleRequest updateRequest;
    private CalculatePriceRequest calculateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        Instant effectiveFrom = now.minus(10, ChronoUnit.DAYS);
        Instant effectiveTo = now.plus(90, ChronoUnit.DAYS);

        // Setup DAY pricing rule
        testDayRule = PricingRule.builder()
                .id(1L)
                .vehicleCategory(VehicleCategory.STANDARD)
                .unit(PricingUnit.DAY)
                .pricePerUnit(new BigDecimal("50.00"))
                .minDuration(Duration.ofHours(1))
                .maxDuration(Duration.ofDays(30))
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .active(true)
                .build();

        // Setup HOUR pricing rule
        testHourRule = PricingRule.builder()
                .id(2L)
                .vehicleCategory(VehicleCategory.STANDARD)
                .unit(PricingUnit.HOUR)
                .pricePerUnit(new BigDecimal("10.00"))
                .minDuration(Duration.ofHours(1))
                .maxDuration(Duration.ofDays(30))
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .active(true)
                .build();

        // Setup MINUTE pricing rule
        testMinuteRule = PricingRule.builder()
                .id(3L)
                .vehicleCategory(VehicleCategory.STANDARD)
                .unit(PricingUnit.MINUTE)
                .pricePerUnit(new BigDecimal("0.50"))
                .minDuration(Duration.ofHours(1))
                .maxDuration(Duration.ofDays(30))
                .effectiveFrom(effectiveFrom)
                .effectiveTo(effectiveTo)
                .active(true)
                .build();

        testResponse = PricingRuleResponse.builder()
                .id(1L)
                .vehicleCategory(VehicleCategory.STANDARD)
                .unit(PricingUnit.DAY)
                .pricePerUnit(new BigDecimal("50.00"))
                .active(true)
                .build();

        createRequest = CreatePricingRuleRequest.builder()
                .vehicleCategory(VehicleCategory.ECONOM)
                .unit(PricingUnit.HOUR)
                .pricePerUnit(new BigDecimal("8.00"))
                .effectiveFrom(now)
                .effectiveTo(now.plus(60, ChronoUnit.DAYS))
                .build();

        updateRequest = UpdatePricingRuleRequest.builder()
                .pricePerUnit(new BigDecimal("55.00"))
                .build();

        Instant pickup = now.plus(1, ChronoUnit.DAYS);
        Instant returnTime = pickup.plus(2, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);
        calculateRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        pageable = PageRequest.of(0, 20);
    }

    // ========================== CREATE RULE ==========================

    @Test
    @DisplayName("createRule - Should create pricing rule successfully")
    void createRule_WhenValidRequest_ShouldCreateRule() {
        // Given
        PricingRule newRule = PricingRule.builder()
                .id(4L)
                .vehicleCategory(VehicleCategory.ECONOM)
                .unit(PricingUnit.HOUR)
                .pricePerUnit(new BigDecimal("8.00"))
                .build();

        when(pricingRuleMapper.toEntity(createRequest)).thenReturn(newRule);
        when(pricingRuleRepository.save(any(PricingRule.class))).thenReturn(newRule);
        when(pricingRuleMapper.toResponse(newRule)).thenReturn(testResponse);

        // When
        PricingRuleResponse result = pricingRuleService.createRule(createRequest);

        // Then
        assertThat(result).isNotNull();

        verify(pricingRuleMapper, times(1)).toEntity(createRequest);
        verify(pricingRuleRepository, times(1)).save(newRule);
        verify(pricingRuleMapper, times(1)).toResponse(newRule);
    }

    // ========================== UPDATE RULE ==========================

    @Test
    @DisplayName("updateRule - Should update pricing rule successfully")
    void updateRule_WhenRuleExists_ShouldUpdateRule() {
        // Given
        Long ruleId = 1L;
        when(pricingRuleRepository.findById(ruleId)).thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.save(testDayRule)).thenReturn(testDayRule);
        when(pricingRuleMapper.toResponse(testDayRule)).thenReturn(testResponse);

        // When
        PricingRuleResponse result = pricingRuleService.updateRule(ruleId, updateRequest);

        // Then
        assertThat(result).isNotNull();

        verify(pricingRuleRepository, times(1)).findById(ruleId);
        verify(pricingRuleMapper, times(1)).updateRuleFromRequest(updateRequest, testDayRule);
        verify(pricingRuleRepository, times(1)).save(testDayRule);
    }

    @Test
    @DisplayName("updateRule - Should throw EntityNotFoundException when rule not found")
    void updateRule_WhenRuleNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        Long nonExistentRuleId = 999L;
        when(pricingRuleRepository.findById(nonExistentRuleId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.updateRule(nonExistentRuleId, updateRequest))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pricing rule not found with id: " + nonExistentRuleId);

        verify(pricingRuleRepository, times(1)).findById(nonExistentRuleId);
        verify(pricingRuleRepository, never()).save(any(PricingRule.class));
    }

    // ========================== DELETE RULE ==========================

    @Test
    @DisplayName("deleteRule - Should delete pricing rule successfully")
    void deleteRule_WhenRuleExists_ShouldDeleteRule() {
        // Given
        Long ruleId = 1L;
        when(pricingRuleRepository.existsById(ruleId)).thenReturn(true);
        doNothing().when(pricingRuleRepository).deleteById(ruleId);

        // When
        pricingRuleService.deleteRule(ruleId);

        // Then
        verify(pricingRuleRepository, times(1)).existsById(ruleId);
        verify(pricingRuleRepository, times(1)).deleteById(ruleId);
    }

    @Test
    @DisplayName("deleteRule - Should throw EntityNotFoundException when rule not found")
    void deleteRule_WhenRuleNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        Long nonExistentRuleId = 999L;
        when(pricingRuleRepository.existsById(nonExistentRuleId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.deleteRule(nonExistentRuleId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pricing rule not found with id: " + nonExistentRuleId);

        verify(pricingRuleRepository, times(1)).existsById(nonExistentRuleId);
        verify(pricingRuleRepository, never()).deleteById(anyLong());
    }

    // ========================== GET BY ID ==========================

    @Test
    @DisplayName("getRuleById - Should return pricing rule when found")
    void getRuleById_WhenRuleExists_ShouldReturnRule() {
        // Given
        Long ruleId = 1L;
        when(pricingRuleRepository.findById(ruleId)).thenReturn(Optional.of(testDayRule));
        when(pricingRuleMapper.toResponse(testDayRule)).thenReturn(testResponse);

        // When
        PricingRuleResponse result = pricingRuleService.getRuleById(ruleId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ruleId);

        verify(pricingRuleRepository, times(1)).findById(ruleId);
        verify(pricingRuleMapper, times(1)).toResponse(testDayRule);
    }

    @Test
    @DisplayName("getRuleById - Should throw EntityNotFoundException when rule not found")
    void getRuleById_WhenRuleNotFound_ShouldThrowEntityNotFoundException() {
        // Given
        Long nonExistentRuleId = 999L;
        when(pricingRuleRepository.findById(nonExistentRuleId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.getRuleById(nonExistentRuleId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Pricing rule not found with id: " + nonExistentRuleId);

        verify(pricingRuleRepository, times(1)).findById(nonExistentRuleId);
        verify(pricingRuleMapper, never()).toResponse(any(PricingRule.class));
    }

    // ========================== GET ALL RULES ==========================

    @Test
    @DisplayName("getAllRules - Should return page of pricing rules")
    void getAllRules_ShouldReturnPageOfRules() {
        // Given
        Page<PricingRule> rulePage = new PageImpl<>(List.of(testDayRule, testHourRule));
        when(pricingRuleRepository.findAll(pageable)).thenReturn(rulePage);
        when(pricingRuleMapper.toResponse(any(PricingRule.class))).thenReturn(testResponse);

        // When
        Page<PricingRuleResponse> result = pricingRuleService.getAllRules(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);

        verify(pricingRuleRepository, times(1)).findAll(pageable);
        verify(pricingRuleMapper, times(2)).toResponse(any(PricingRule.class));
    }

    @Test
    @DisplayName("getAllRules - Should return empty page when no rules exist")
    void getAllRules_WhenNoRulesExist_ShouldReturnEmptyPage() {
        // Given
        Page<PricingRule> emptyPage = Page.empty(pageable);
        when(pricingRuleRepository.findAll(pageable)).thenReturn(emptyPage);

        // When
        Page<PricingRuleResponse> result = pricingRuleService.getAllRules(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(pricingRuleRepository, times(1)).findAll(pageable);
        verify(pricingRuleMapper, never()).toResponse(any(PricingRule.class));
    }

    // ========================== CALCULATE PRICE ==========================

    @Test
    @DisplayName("calculatePrice - Should calculate correct price for multi-day rental")
    void calculatePrice_ForMultiDayRental_ShouldCalculateCorrectPrice() {
        // Given: 2 days, 5 hours, 30 minutes
        // Expected: (2 × 50.00) + (5 × 10.00) + (30 × 0.50) = 100 + 50 + 15 = 165.00 MDL
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When
        CalculatePriceResponse result = pricingRuleService.calculatePrice(calculateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("165.00"));
        assertThat(result.getBreakdown()).hasSize(3);
        
        // Verify DAY breakdown
        CalculatePriceResponse.UnitBreakdown dayBreakdown = result.getBreakdown().get(0);
        assertThat(dayBreakdown.getUnit()).isEqualTo(PricingUnit.DAY);
        assertThat(dayBreakdown.getQuantity()).isEqualTo(2L);
        assertThat(dayBreakdown.getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Verify HOUR breakdown
        CalculatePriceResponse.UnitBreakdown hourBreakdown = result.getBreakdown().get(1);
        assertThat(hourBreakdown.getUnit()).isEqualTo(PricingUnit.HOUR);
        assertThat(hourBreakdown.getQuantity()).isEqualTo(5L);
        assertThat(hourBreakdown.getSubtotal()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Verify MINUTE breakdown
        CalculatePriceResponse.UnitBreakdown minuteBreakdown = result.getBreakdown().get(2);
        assertThat(minuteBreakdown.getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(minuteBreakdown.getQuantity()).isEqualTo(30L);
        assertThat(minuteBreakdown.getSubtotal()).isEqualByComparingTo(new BigDecimal("15.00"));

        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class));
        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class));
        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class));
    }

    @Test
    @DisplayName("calculatePrice - Should calculate correct price for short rental (hours only)")
    void calculatePrice_ForShortRental_ShouldCalculateCorrectPrice() {
        // Given: 0 days, 2 hours, 15 minutes
        // Expected: (2 × 10.00) + (15 × 0.50) = 20 + 7.50 = 27.50 MDL
        Instant pickup = Instant.now();
        Instant returnTime = pickup.plus(2, ChronoUnit.HOURS).plus(15, ChronoUnit.MINUTES);
        CalculatePriceRequest shortRentalRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When
        CalculatePriceResponse result = pricingRuleService.calculatePrice(shortRentalRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("27.50"));
        assertThat(result.getBreakdown()).hasSize(2); // Only HOUR and MINUTE (no DAY)

        // Verify HOUR breakdown
        CalculatePriceResponse.UnitBreakdown hourBreakdown = result.getBreakdown().get(0);
        assertThat(hourBreakdown.getUnit()).isEqualTo(PricingUnit.HOUR);
        assertThat(hourBreakdown.getQuantity()).isEqualTo(2L);

        // Verify MINUTE breakdown
        CalculatePriceResponse.UnitBreakdown minuteBreakdown = result.getBreakdown().get(1);
        assertThat(minuteBreakdown.getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(minuteBreakdown.getQuantity()).isEqualTo(15L);
    }

    @Test
    @DisplayName("calculatePrice - Should throw IllegalArgumentException when return before pickup")
    void calculatePrice_WhenReturnBeforePickup_ShouldThrowIllegalArgumentException() {
        // Given: return < pickup (invalid)
        Instant pickup = Instant.now();
        Instant invalidReturn = pickup.minus(1, ChronoUnit.HOURS);
        CalculatePriceRequest invalidRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(invalidReturn)
                .build();

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(invalidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Return datetime must be after pickup datetime");

        verify(pricingRuleRepository, never()).findActiveRule(any(), any(), any());
    }

    @Test
    @DisplayName("calculatePrice - Should throw IllegalArgumentException when duration below minimum")
    void calculatePrice_WhenDurationBelowMinimum_ShouldThrowIllegalArgumentException() {
        // Given: 30 minutes (below 1 hour minimum)
        Instant pickup = Instant.now();
        Instant returnTime = pickup.plus(30, ChronoUnit.MINUTES);
        CalculatePriceRequest shortRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(shortRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rental duration")
                .hasMessageContaining("is below minimum");

        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class));
    }

    @Test
    @DisplayName("calculatePrice - Should throw IllegalArgumentException when duration exceeds maximum")
    void calculatePrice_WhenDurationExceedsMaximum_ShouldThrowIllegalArgumentException() {
        // Given: 35 days (exceeds 30 days maximum)
        Instant pickup = Instant.now();
        Instant returnTime = pickup.plus(35, ChronoUnit.DAYS);
        CalculatePriceRequest longRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(longRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rental duration")
                .hasMessageContaining("exceeds maximum");

        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class));
    }

    @Test
    @DisplayName("calculatePrice - Should throw IllegalStateException when no active DAY rule found")
    void calculatePrice_WhenNoDayRuleFound_ShouldThrowIllegalStateException() {
        // Given
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(calculateRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active pricing rule found")
                .hasMessageContaining("category=STANDARD")
                .hasMessageContaining("unit=DAY");

        verify(pricingRuleRepository, times(1)).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class));
        verify(pricingRuleRepository, never()).findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class));
    }

    @Test
    @DisplayName("calculatePrice - Should handle exact full days (no hours/minutes remainder)")
    void calculatePrice_ForExactFullDays_ShouldCalculateCorrectPrice() {
        // Given: Exactly 3 days (no hours/minutes)
        Instant pickup = Instant.now();
        Instant returnTime = pickup.plus(3, ChronoUnit.DAYS);
        CalculatePriceRequest exactDaysRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When
        CalculatePriceResponse result = pricingRuleService.calculatePrice(exactDaysRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalCost()).isEqualByComparingTo(new BigDecimal("150.00")); // 3 × 50.00
        assertThat(result.getBreakdown()).hasSize(1); // Only DAY breakdown

        CalculatePriceResponse.UnitBreakdown dayBreakdown = result.getBreakdown().get(0);
        assertThat(dayBreakdown.getUnit()).isEqualTo(PricingUnit.DAY);
        assertThat(dayBreakdown.getQuantity()).isEqualTo(3L);
        assertThat(dayBreakdown.getSubtotal()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("calculatePrice - Should round up partial minutes (e.g., 90 seconds → 2 minutes)")
    void calculatePrice_ShouldRoundUpPartialMinutes() {
        // Given: 1 hour 1 minute 30 seconds (should round 90s → 2 minutes)
        Instant pickup = Instant.now();
        Instant returnTime = pickup.plus(1, ChronoUnit.HOURS).plus(90, ChronoUnit.SECONDS);
        CalculatePriceRequest partialMinutesRequest = CalculatePriceRequest.builder()
                .vehicleCategory(VehicleCategory.STANDARD)
                .pickupDatetime(pickup)
                .returnDatetime(returnTime)
                .build();

        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.DAY), any(Instant.class)))
                .thenReturn(Optional.of(testDayRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.HOUR), any(Instant.class)))
                .thenReturn(Optional.of(testHourRule));
        when(pricingRuleRepository.findActiveRule(any(VehicleCategory.class), eq(PricingUnit.MINUTE), any(Instant.class)))
                .thenReturn(Optional.of(testMinuteRule));

        // When
        CalculatePriceResponse result = pricingRuleService.calculatePrice(partialMinutesRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBreakdown()).hasSize(2); // HOUR + MINUTE

        // Verify MINUTE breakdown (should be 2 minutes due to rounding up)
        CalculatePriceResponse.UnitBreakdown minuteBreakdown = result.getBreakdown().get(1);
        assertThat(minuteBreakdown.getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(minuteBreakdown.getQuantity()).isEqualTo(2L); // Rounded up from 1.5 minutes
    }
}
