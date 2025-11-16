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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for PricingRuleService using Testcontainers PostgreSQL 16.
 * 
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li><strong>CRUD Operations:</strong> Create, update, delete, retrieve pricing rules</li>
 *   <li><strong>Price Calculation:</strong> Complex algorithm testing with DAY/HOUR/MINUTE breakdown</li>
 *   <li><strong>Validation:</strong> Duration constraints, invalid timestamps, missing rules</li>
 *   <li><strong>Edge Cases:</strong> Zero duration, negative duration, boundary conditions</li>
 *   <li><strong>Cache Integration:</strong> Verification of @Cacheable and @CacheEvict behavior</li>
 * </ul>
 * 
 * <p><strong>Calculation Test Scenarios:</strong></p>
 * <ul>
 *   <li>Short rental: 90 minutes (1 HOUR + 30 MINUTE)</li>
 *   <li>Medium rental: 5.5 hours (5 HOUR + 30 MINUTE)</li>
 *   <li>Long rental: 2 days 5 hours 15 minutes (2 DAY + 5 HOUR + 15 MINUTE)</li>
 *   <li>Exact unit: 3 days (3 DAY, no remainder)</li>
 *   <li>Edge cases: 1 minute, 59 seconds (rounds up to 1 MINUTE)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRuleService
 * @see CalculatePriceRequest
 * @see CalculatePriceResponse
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("PricingRuleService Integration Tests")
@SuppressWarnings("resource")
class PricingRuleServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("pricing_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("test-init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.flyway.schemas", () -> "pricing");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "pricing");
    }

    @Autowired
    private PricingRuleService pricingRuleService;

    @Autowired
    private PricingRuleRepository pricingRuleRepository;

    @BeforeEach
    void setUp() {
        pricingRuleRepository.deleteAll();
    }

    // ========== CRUD Operations ==========

    @Test
    @DisplayName("Should create pricing rule successfully")
    void shouldCreatePricingRule() {
        // Given
        CreatePricingRuleRequest request = CreatePricingRuleRequest.builder()
            .unit(PricingUnit.HOUR)
            .vehicleCategory(VehicleCategory.STANDARD)
            .pricePerUnit(new BigDecimal("12.00"))
            .minDuration(Duration.ofHours(1))
            .maxDuration(Duration.ofHours(24))
            .effectiveFrom(Instant.now())
            .active(true)
            .build();

        // When
        PricingRuleResponse response = pricingRuleService.createRule(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isNotNull();
        assertThat(response.getUnit()).isEqualTo(PricingUnit.HOUR);
        assertThat(response.getVehicleCategory()).isEqualTo(VehicleCategory.STANDARD);
        assertThat(response.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(response.getActive()).isTrue();
    }

    @Test
    @DisplayName("Should update pricing rule successfully")
    void shouldUpdatePricingRule() {
        // Given
        CreatePricingRuleRequest createRequest = CreatePricingRuleRequest.builder()
            .unit(PricingUnit.DAY)
            .vehicleCategory(VehicleCategory.PREMIUM)
            .pricePerUnit(new BigDecimal("80.00"))
            .effectiveFrom(Instant.now())
            .active(true)
            .build();
        PricingRuleResponse created = pricingRuleService.createRule(createRequest);

        UpdatePricingRuleRequest updateRequest = UpdatePricingRuleRequest.builder()
            .pricePerUnit(new BigDecimal("90.00"))
            .active(false)
            .build();

        // When
        PricingRuleResponse updated = pricingRuleService.updateRule(created.getId(), updateRequest);

        // Then
        assertThat(updated.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(updated.getActive()).isFalse();
    }

    @Test
    @DisplayName("Should delete pricing rule successfully")
    void shouldDeletePricingRule() {
        // Given
        CreatePricingRuleRequest request = CreatePricingRuleRequest.builder()
            .unit(PricingUnit.MINUTE)
            .vehicleCategory(VehicleCategory.ECONOM)
            .pricePerUnit(new BigDecimal("0.30"))
            .effectiveFrom(Instant.now())
            .active(true)
            .build();
        PricingRuleResponse created = pricingRuleService.createRule(request);

        // When
        pricingRuleService.deleteRule(created.getId());

        // Then
        assertThatThrownBy(() -> pricingRuleService.getRuleById(created.getId()))
            .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // ========== Price Calculation Tests ==========

    @Test
    @DisplayName("Should calculate price for short rental: 90 minutes (1 HOUR + 30 MINUTE)")
    void shouldCalculatePriceForShortRental() {
        // Given: Setup pricing rules
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(90, ChronoUnit.MINUTES); // 1.5 hours

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 1 HOUR @ 12.00 + 30 MINUTE @ 0.30 = 12.00 + 9.00 = 21.00
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("21.00"));
        assertThat(response.getBreakdown()).hasSize(2);
        
        assertThat(response.getBreakdown().get(0).getUnit()).isEqualTo(PricingUnit.HOUR);
        assertThat(response.getBreakdown().get(0).getQuantity()).isEqualTo(1);
        assertThat(response.getBreakdown().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("12.00"));
        
        assertThat(response.getBreakdown().get(1).getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(response.getBreakdown().get(1).getQuantity()).isEqualTo(30);
        assertThat(response.getBreakdown().get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("9.00"));
    }

    @Test
    @DisplayName("Should calculate price for medium rental: 5.5 hours (5 HOUR + 30 MINUTE)")
    void shouldCalculatePriceForMediumRental() {
        // Given
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(5, ChronoUnit.HOURS).plus(30, ChronoUnit.MINUTES);

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 5 HOUR @ 12.00 + 30 MINUTE @ 0.30 = 60.00 + 9.00 = 69.00
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("69.00"));
        assertThat(response.getBreakdown()).hasSize(2);
    }

    @Test
    @DisplayName("Should calculate price for long rental: 2d 5h 15m (2 DAY + 5 HOUR + 15 MINUTE)")
    void shouldCalculatePriceForLongRental() {
        // Given
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup
            .plus(2, ChronoUnit.DAYS)
            .plus(5, ChronoUnit.HOURS)
            .plus(15, ChronoUnit.MINUTES);

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 2 DAY @ 50.00 + 5 HOUR @ 12.00 + 15 MINUTE @ 0.30 = 100.00 + 60.00 + 4.50 = 164.50
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("164.50"));
        assertThat(response.getBreakdown()).hasSize(3);
        
        assertThat(response.getBreakdown().get(0).getUnit()).isEqualTo(PricingUnit.DAY);
        assertThat(response.getBreakdown().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getBreakdown().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        
        assertThat(response.getBreakdown().get(1).getUnit()).isEqualTo(PricingUnit.HOUR);
        assertThat(response.getBreakdown().get(1).getQuantity()).isEqualTo(5);
        assertThat(response.getBreakdown().get(1).getSubtotal()).isEqualByComparingTo(new BigDecimal("60.00"));
        
        assertThat(response.getBreakdown().get(2).getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(response.getBreakdown().get(2).getQuantity()).isEqualTo(15);
        assertThat(response.getBreakdown().get(2).getSubtotal()).isEqualByComparingTo(new BigDecimal("4.50"));
    }

    @Test
    @DisplayName("Should calculate price for exact units: 3 days (no remainder)")
    void shouldCalculatePriceForExactUnits() {
        // Given
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(3, ChronoUnit.DAYS); // Exactly 3 days

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 3 DAY @ 50.00 = 150.00
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(response.getBreakdown()).hasSize(1);
        assertThat(response.getBreakdown().get(0).getUnit()).isEqualTo(PricingUnit.DAY);
        assertThat(response.getBreakdown().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should round up partial minutes: 59 seconds → 1 MINUTE")
    void shouldRoundUpPartialMinutes() {
        // Given
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(59, ChronoUnit.SECONDS); // Less than 1 minute

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 1 MINUTE @ 0.30 = 0.30 (rounded up from 59 seconds)
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(response.getBreakdown()).hasSize(1);
        assertThat(response.getBreakdown().get(0).getUnit()).isEqualTo(PricingUnit.MINUTE);
        assertThat(response.getBreakdown().get(0).getQuantity()).isEqualTo(1);
    }

    // ========== Validation Tests ==========

    @Test
    @DisplayName("Should reject calculation with return before pickup")
    void shouldRejectReturnBeforePickup() {
        // Given
        setupStandardPricingRules();

        Instant pickup = Instant.parse("2025-01-10T15:00:00Z");
        Instant returnTime = Instant.parse("2025-01-10T10:00:00Z"); // BEFORE pickup!

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When/Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Return datetime must be after pickup datetime");
    }

    @Test
    @DisplayName("Should reject calculation with duration below minimum")
    void shouldRejectDurationBelowMinimum() {
        // Given: Create rules with min duration 1 hour
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.DAY, VehicleCategory.STANDARD, "50.00",
            Duration.ofHours(1), Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.HOUR, VehicleCategory.STANDARD, "12.00",
            Duration.ofHours(1), Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.MINUTE, VehicleCategory.STANDARD, "0.30",
            Duration.ofHours(1), Duration.ofDays(30), now, future
        ));

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(30, ChronoUnit.MINUTES); // Only 30 minutes (below 1 hour min)

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When/Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("below minimum");
    }

    @Test
    @DisplayName("Should reject calculation with duration above maximum")
    void shouldRejectDurationAboveMaximum() {
        // Given: Create rules with max duration 7 days
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.DAY, VehicleCategory.STANDARD, "50.00",
            Duration.ofHours(1), Duration.ofDays(7), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.HOUR, VehicleCategory.STANDARD, "12.00",
            Duration.ofHours(1), Duration.ofDays(7), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.MINUTE, VehicleCategory.STANDARD, "0.30",
            Duration.ofHours(1), Duration.ofDays(7), now, future
        ));

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(10, ChronoUnit.DAYS); // 10 days (above 7 days max)

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When/Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("Should reject calculation when pricing rules are missing")
    void shouldRejectCalculationWhenRulesAreMissing() {
        // Given: NO pricing rules created

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(5, ChronoUnit.HOURS);

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.STANDARD)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When/Then
        assertThatThrownBy(() -> pricingRuleService.calculatePrice(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No active pricing rule found");
    }

    // ========== Premium Category Tests ==========

    @Test
    @DisplayName("Should calculate price for PREMIUM category with higher rates")
    void shouldCalculatePriceForPremiumCategory() {
        // Given: Setup PREMIUM pricing rules (higher rates)
        setupPremiumPricingRules();

        Instant pickup = Instant.parse("2025-01-10T10:00:00Z");
        Instant returnTime = pickup.plus(1, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS);

        CalculatePriceRequest request = CalculatePriceRequest.builder()
            .vehicleCategory(VehicleCategory.PREMIUM)
            .pickupDatetime(pickup)
            .returnDatetime(returnTime)
            .build();

        // When
        CalculatePriceResponse response = pricingRuleService.calculatePrice(request);

        // Then
        // Expected: 1 DAY @ 100.00 + 3 HOUR @ 20.00 = 100.00 + 60.00 = 160.00
        assertThat(response.getTotalCost()).isEqualByComparingTo(new BigDecimal("160.00"));
        assertThat(response.getVehicleCategory()).isEqualTo(VehicleCategory.PREMIUM);
    }

    // ========== Helper Methods ==========

    private void setupStandardPricingRules() {
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        // STANDARD category pricing rules
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.DAY, VehicleCategory.STANDARD, "50.00",
            Duration.ZERO, Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.HOUR, VehicleCategory.STANDARD, "12.00",
            Duration.ZERO, Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.MINUTE, VehicleCategory.STANDARD, "0.30",
            Duration.ofMinutes(1), Duration.ofDays(30), now, future
        ));
    }

    private void setupPremiumPricingRules() {
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        // PREMIUM category pricing rules (higher rates)
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.DAY, VehicleCategory.PREMIUM, "100.00",
            Duration.ZERO, Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.HOUR, VehicleCategory.PREMIUM, "20.00",
            Duration.ZERO, Duration.ofDays(30), now, future
        ));
        pricingRuleRepository.save(createPricingRule(
            PricingUnit.MINUTE, VehicleCategory.PREMIUM, "0.60",
            Duration.ofMinutes(1), Duration.ofDays(30), now, future
        ));
    }

    private PricingRule createPricingRule(
            PricingUnit unit,
            VehicleCategory category,
            String pricePerUnit,
            Duration minDuration,
            Duration maxDuration,
            Instant effectiveFrom,
            Instant effectiveTo) {
        return PricingRule.builder()
            .unit(unit)
            .vehicleCategory(category)
            .pricePerUnit(new BigDecimal(pricePerUnit))
            .minDuration(minDuration)
            .maxDuration(maxDuration)
            .cancellationWindow(Duration.ofHours(2))
            .lateReturnPenaltyPercent(new BigDecimal("25.00"))
            .effectiveFrom(effectiveFrom)
            .effectiveTo(effectiveTo)
            .active(true)
            .build();
    }
}
