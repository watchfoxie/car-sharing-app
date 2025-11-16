package com.services.pricing_rules_service.domain.repository;

import com.services.pricing_rules_service.domain.entity.PricingRule;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for PricingRuleRepository using Testcontainers PostgreSQL 16.
 * 
 * <p><strong>Test Coverage:</strong></p>
 * <ul>
 *   <li><strong>CRUD Operations:</strong> Create, read, update, delete pricing rules</li>
 *   <li><strong>EXCLUDE Constraint:</strong> Temporal overlap prevention for (category, unit, effective_period)</li>
 *   <li><strong>Active Rule Queries:</strong> Finding rules by category, unit, timestamp</li>
 *   <li><strong>Expired Rule Detection:</strong> Querying rules past their effectiveTo date</li>
 *   <li><strong>Audit Fields:</strong> Verification of createdAt, lastModifiedAt population</li>
 *   <li><strong>Generated Columns:</strong> effective_period (tstzrange) auto-computation</li>
 * </ul>
 * 
 * <p><strong>PostgreSQL Extensions Used:</strong></p>
 * <ul>
 *   <li><strong>btree_gist:</strong> Enables EXCLUDE constraint on temporal ranges</li>
 *   <li><strong>citext:</strong> Case-insensitive text comparison (if applicable)</li>
 * </ul>
 * 
 * <p><strong>Test Strategy:</strong></p>
 * <ul>
 *   <li>Each test method clears database state via {@code deleteAll()} in {@code @BeforeEach}</li>
 *   <li>Testcontainers provides isolated PostgreSQL 16 instance per test run</li>
 *   <li>Flyway migrations run automatically via {@code @DataJpaTest}</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRuleRepository
 * @see PricingRule
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("PricingRuleRepository Integration Tests")
@SuppressWarnings("resource")
@Import(com.services.pricing_rules_service.config.JpaAuditingConfig.class)
class PricingRuleRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("pricing_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("test-init.sql"); // Creates pricing schema + extensions

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
    private PricingRuleRepository pricingRuleRepository;

    @BeforeEach
    void setUp() {
        pricingRuleRepository.deleteAll();
    }

    // ========== CRUD Operations ==========

    @Test
    @DisplayName("Should save and retrieve pricing rule with all fields")
    void shouldSaveAndRetrievePricingRule() {
        // Given
        PricingRule rule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            Instant.now(),
            Instant.now().plus(365, ChronoUnit.DAYS)
        );

        // When
        PricingRule savedRule = pricingRuleRepository.save(rule);
        Optional<PricingRule> retrievedRule = pricingRuleRepository.findById(savedRule.getId());

        // Then
        assertThat(retrievedRule).isPresent();
        assertThat(retrievedRule.get())
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "lastModifiedAt") // Audit fields populated by DB
            .isEqualTo(savedRule);
        
        // Verify audit fields are populated
        assertThat(retrievedRule.get().getCreatedAt()).isNotNull();
        assertThat(retrievedRule.get().getLastModifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update pricing rule and modify lastModifiedAt")
    void shouldUpdatePricingRule() throws InterruptedException {
        // Given
        PricingRule rule = createTestRule(
            PricingUnit.DAY,
            VehicleCategory.PREMIUM,
            new BigDecimal("80.00"),
            Instant.now(),
            null
        );
        PricingRule savedRule = pricingRuleRepository.save(rule);
        Instant originalModifiedAt = savedRule.getLastModifiedAt();

        // Wait to ensure timestamp difference
        Thread.sleep(100);

        // When
        savedRule.setPricePerUnit(new BigDecimal("90.00"));
        pricingRuleRepository.saveAndFlush(savedRule);
        PricingRule updatedRule = pricingRuleRepository.findById(savedRule.getId()).orElseThrow();

        // Then
        assertThat(updatedRule.getPricePerUnit()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(updatedRule.getLastModifiedAt()).isAfter(originalModifiedAt);
    }

    @Test
    @DisplayName("Should delete pricing rule")
    void shouldDeletePricingRule() {
        // Given
        PricingRule rule = createTestRule(
            PricingUnit.MINUTE,
            VehicleCategory.ECONOM,
            new BigDecimal("0.30"),
            Instant.now(),
            null
        );
        PricingRule savedRule = pricingRuleRepository.save(rule);

        // When
        pricingRuleRepository.deleteById(savedRule.getId());

        // Then
        Optional<PricingRule> deletedRule = pricingRuleRepository.findById(savedRule.getId());
        assertThat(deletedRule).isEmpty();
    }

    // ========== EXCLUDE Constraint Tests ==========

    @Test
    @DisplayName("Should prevent overlapping rules for same category and unit - EXCLUDE constraint")
    void shouldPreventOverlappingRules() {
        // Given
        Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
        
        // Rule 1: STANDARD HOUR, effective Jan 1 - Dec 31, 2025
        PricingRule rule1 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            baseTime,
            baseTime.plus(365, ChronoUnit.DAYS)
        );
        pricingRuleRepository.save(rule1);

        // Rule 2: STANDARD HOUR, effective Jun 1 - Dec 31, 2025 (OVERLAPS with rule1!)
        PricingRule rule2 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("15.00"),
            baseTime.plus(150, ChronoUnit.DAYS), // Jun 1
            baseTime.plus(365, ChronoUnit.DAYS)  // Dec 31
        );

        // When/Then
        assertThatThrownBy(() -> pricingRuleRepository.save(rule2))
            .isInstanceOf(DataIntegrityViolationException.class)
            .hasMessageContaining("ex_pricing_no_overlap");
    }

    @Test
    @DisplayName("Should allow non-overlapping rules for same category and unit")
    void shouldAllowNonOverlappingRules() {
        // Given
        Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
        
        // Rule 1: STANDARD HOUR, effective Jan 1 - Jun 30, 2025
        PricingRule rule1 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            baseTime,
            baseTime.plus(180, ChronoUnit.DAYS) // Jun 30
        );
        pricingRuleRepository.save(rule1);

        // Rule 2: STANDARD HOUR, effective Jul 1 - Dec 31, 2025 (NO OVERLAP!)
        PricingRule rule2 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("15.00"),
            baseTime.plus(181, ChronoUnit.DAYS), // Jul 1
            baseTime.plus(365, ChronoUnit.DAYS)  // Dec 31
        );

        // When
        PricingRule savedRule2 = pricingRuleRepository.save(rule2);

        // Then
        assertThat(savedRule2.getId()).isNotNull();
        assertThat(pricingRuleRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should allow overlapping rules for different categories")
    void shouldAllowOverlappingRulesForDifferentCategories() {
        // Given
        Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
        Instant endTime = baseTime.plus(365, ChronoUnit.DAYS);
        
        // Rule 1: STANDARD HOUR
        PricingRule rule1 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            baseTime,
            endTime
        );
        pricingRuleRepository.save(rule1);

        // Rule 2: PREMIUM HOUR (different category, same period - ALLOWED)
        PricingRule rule2 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.PREMIUM,
            new BigDecimal("20.00"),
            baseTime,
            endTime
        );

        // When
        PricingRule savedRule2 = pricingRuleRepository.save(rule2);

        // Then
        assertThat(savedRule2.getId()).isNotNull();
        assertThat(pricingRuleRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should allow overlapping rules for different units")
    void shouldAllowOverlappingRulesForDifferentUnits() {
        // Given
        Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
        Instant endTime = baseTime.plus(365, ChronoUnit.DAYS);
        
        // Rule 1: STANDARD HOUR
        PricingRule rule1 = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            baseTime,
            endTime
        );
        pricingRuleRepository.save(rule1);

        // Rule 2: STANDARD DAY (different unit, same period - ALLOWED)
        PricingRule rule2 = createTestRule(
            PricingUnit.DAY,
            VehicleCategory.STANDARD,
            new BigDecimal("80.00"),
            baseTime,
            endTime
        );

        // When
        PricingRule savedRule2 = pricingRuleRepository.save(rule2);

        // Then
        assertThat(savedRule2.getId()).isNotNull();
        assertThat(pricingRuleRepository.count()).isEqualTo(2);
    }

    // ========== Active Rule Queries ==========

    @Test
    @DisplayName("Should find active rule by category, unit, and timestamp")
    void shouldFindActiveRule() {
        // Given
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);
        Instant future = now.plus(30, ChronoUnit.DAYS);
        
        PricingRule rule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            past,
            future
        );
        rule.setActive(true);
        pricingRuleRepository.save(rule);

        // When
        Optional<PricingRule> foundRule = pricingRuleRepository.findActiveRule(
            VehicleCategory.STANDARD,
            PricingUnit.HOUR,
            now
        );

        // Then
        assertThat(foundRule).isPresent();
        assertThat(foundRule.get().getPricePerUnit()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    @Test
    @DisplayName("Should not find inactive rule")
    void shouldNotFindInactiveRule() {
        // Given
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);
        Instant future = now.plus(30, ChronoUnit.DAYS);
        
        PricingRule rule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            past,
            future
        );
        rule.setActive(false); // Inactive!
        pricingRuleRepository.save(rule);

        // When
        Optional<PricingRule> foundRule = pricingRuleRepository.findActiveRule(
            VehicleCategory.STANDARD,
            PricingUnit.HOUR,
            now
        );

        // Then
        assertThat(foundRule).isEmpty();
    }

    @Test
    @DisplayName("Should not find rule outside effective period")
    void shouldNotFindRuleOutsideEffectivePeriod() {
        // Given
        Instant baseTime = Instant.parse("2025-06-01T00:00:00Z");
        Instant startTime = baseTime;
        Instant endTime = baseTime.plus(30, ChronoUnit.DAYS);
        Instant queryTime = baseTime.plus(60, ChronoUnit.DAYS); // After endTime!
        
        PricingRule rule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            startTime,
            endTime
        );
        rule.setActive(true);
        pricingRuleRepository.save(rule);

        // When
        Optional<PricingRule> foundRule = pricingRuleRepository.findActiveRule(
            VehicleCategory.STANDARD,
            PricingUnit.HOUR,
            queryTime
        );

        // Then
        assertThat(foundRule).isEmpty();
    }

    @Test
    @DisplayName("Should find rule with null effectiveTo (open-ended)")
    void shouldFindRuleWithNullEffectiveTo() {
        // Given
        Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
        Instant queryTime = baseTime.plus(365, ChronoUnit.DAYS);
        
        PricingRule rule = createTestRule(
            PricingUnit.DAY,
            VehicleCategory.PREMIUM,
            new BigDecimal("100.00"),
            baseTime,
            null // Open-ended!
        );
        rule.setActive(true);
        pricingRuleRepository.save(rule);

        // When
        Optional<PricingRule> foundRule = pricingRuleRepository.findActiveRule(
            VehicleCategory.PREMIUM,
            PricingUnit.DAY,
            queryTime
        );

        // Then
        assertThat(foundRule).isPresent();
        assertThat(foundRule.get().getEffectiveTo()).isNull();
    }

    // ========== Expired Rule Detection ==========

    @Test
    @DisplayName("Should find expired rules")
    void shouldFindExpiredRules() {
        // Given
        Instant now = Instant.now();
        Instant past = now.minus(60, ChronoUnit.DAYS);
        Instant expiredEnd = now.minus(10, ChronoUnit.DAYS); // Expired 10 days ago
        
        PricingRule expiredRule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("10.00"),
            past,
            expiredEnd
        );
        expiredRule.setActive(true); // Still marked active (should be cleaned up)
        pricingRuleRepository.save(expiredRule);

        // When
        List<PricingRule> expiredRules = pricingRuleRepository.findExpiredRules(now);

        // Then
        assertThat(expiredRules).hasSize(1);
        assertThat(expiredRules.get(0).getEffectiveTo()).isBefore(now);
        assertThat(expiredRules.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("Should not find non-expired rules")
    void shouldNotFindNonExpiredRules() {
        // Given
        Instant now = Instant.now();
        Instant past = now.minus(30, ChronoUnit.DAYS);
        Instant futureEnd = now.plus(30, ChronoUnit.DAYS); // Still valid
        
        PricingRule activeRule = createTestRule(
            PricingUnit.HOUR,
            VehicleCategory.STANDARD,
            new BigDecimal("12.00"),
            past,
            futureEnd
        );
        activeRule.setActive(true);
        pricingRuleRepository.save(activeRule);

        // When
        List<PricingRule> expiredRules = pricingRuleRepository.findExpiredRules(now);

        // Then
        assertThat(expiredRules).isEmpty();
    }

    // ========== Filtering by Category/Unit ==========

    @Test
    @DisplayName("Should find all active rules by category")
    void shouldFindActiveRulesByCategory() {
        // Given
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.DAY, VehicleCategory.STANDARD, "50.00", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.HOUR, VehicleCategory.STANDARD, "10.00", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.MINUTE, VehicleCategory.STANDARD, "0.30", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.DAY, VehicleCategory.PREMIUM, "100.00", now, future));

        // When
        List<PricingRule> standardRules = pricingRuleRepository.findActiveRulesByCategory(VehicleCategory.STANDARD);

        // Then
        assertThat(standardRules).hasSize(3);
        assertThat(standardRules).allMatch(rule -> rule.getVehicleCategory() == VehicleCategory.STANDARD);
    }

    @Test
    @DisplayName("Should find all active rules by unit")
    void shouldFindActiveRulesByUnit() {
        // Given
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.HOUR, VehicleCategory.STANDARD, "12.00", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.HOUR, VehicleCategory.PREMIUM, "20.00", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.DAY, VehicleCategory.STANDARD, "80.00", now, future));

        // When
        List<PricingRule> hourRules = pricingRuleRepository.findActiveRulesByUnit(PricingUnit.HOUR);

        // Then
        assertThat(hourRules).hasSize(2);
        assertThat(hourRules).allMatch(rule -> rule.getUnit() == PricingUnit.HOUR);
    }

    // ========== Count and Existence Checks ==========

    @Test
    @DisplayName("Should count active rules")
    void shouldCountActiveRules() {
        // Given
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.DAY, VehicleCategory.STANDARD, "50.00", now, future));
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.HOUR, VehicleCategory.PREMIUM, "20.00", now, future));
        
        PricingRule inactiveRule = createTestRule(PricingUnit.MINUTE, VehicleCategory.ECONOM, new BigDecimal("0.20"), now, future);
        inactiveRule.setActive(false);
        pricingRuleRepository.save(inactiveRule);

        // When
        long activeCount = pricingRuleRepository.countActiveRules();

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check existence of active rule")
    void shouldCheckExistenceOfActiveRule() {
        // Given
        Instant now = Instant.now();
        Instant future = now.plus(365, ChronoUnit.DAYS);
        
        pricingRuleRepository.save(createActiveTestRule(PricingUnit.HOUR, VehicleCategory.STANDARD, "12.00", now, future));

        // When
        boolean exists = pricingRuleRepository.existsActiveRule(VehicleCategory.STANDARD, PricingUnit.HOUR, now);
        boolean notExists = pricingRuleRepository.existsActiveRule(VehicleCategory.PREMIUM, PricingUnit.HOUR, now);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    // ========== Helper Methods ==========

    private PricingRule createTestRule(
            PricingUnit unit,
            VehicleCategory category,
            BigDecimal pricePerUnit,
            Instant effectiveFrom,
            Instant effectiveTo) {
        return PricingRule.builder()
            .unit(unit)
            .vehicleCategory(category)
            .pricePerUnit(pricePerUnit)
            .minDuration(Duration.ofMinutes(30))
            .maxDuration(Duration.ofDays(30))
            .cancellationWindow(Duration.ofHours(2))
            .lateReturnPenaltyPercent(new BigDecimal("25.00"))
            .effectiveFrom(effectiveFrom)
            .effectiveTo(effectiveTo)
            .active(false) // Default inactive (set explicitly in tests)
            .build();
    }

    private PricingRule createActiveTestRule(
            PricingUnit unit,
            VehicleCategory category,
            String pricePerUnit,
            Instant effectiveFrom,
            Instant effectiveTo) {
        PricingRule rule = createTestRule(unit, category, new BigDecimal(pricePerUnit), effectiveFrom, effectiveTo);
        rule.setActive(true);
        return rule;
    }
}
