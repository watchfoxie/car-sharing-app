package com.services.pricing_rules_service.domain.repository;

import com.services.pricing_rules_service.domain.entity.PricingRule;
import com.services.pricing_rules_service.domain.enums.PricingUnit;
import com.services.pricing_rules_service.domain.enums.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PricingRule} entities.
 * 
 * <p>This repository provides data access methods for pricing rules with support for:</p>
 * <ul>
 *   <li>CRUD operations (inherited from {@link JpaRepository})</li>
 *   <li>Querying active rules by vehicle category and time unit</li>
 *   <li>Temporal queries based on effective validity periods</li>
 *   <li>Overlap detection for preventing conflicting rules</li>
 * </ul>
 * 
 * <p><strong>Key Query Methods:</strong></p>
 * <ul>
 *   <li>{@link #findActiveRule(VehicleCategory, PricingUnit, Instant)} - Finds the applicable rule for a specific timestamp</li>
 *   <li>{@link #findAllActiveRules()} - Retrieves all currently active rules (for cache warm-up)</li>
 *   <li>{@link #findExpiredRules(Instant)} - Identifies rules that have passed their effective_to date (for cleanup jobs)</li>
 * </ul>
 * 
 * <p><strong>Caching Strategy:</strong></p>
 * <ul>
 *   <li>Results from {@code findActiveRule()} are cached in Redis (key: {@code pricingRules::{category}::{unit}})</li>
 *   <li>Cache invalidation occurs on save/delete operations (handled by {@code PricingRuleService})</li>
 * </ul>
 * 
 * <p><strong>Concurrency & Constraints:</strong></p>
 * <ul>
 *   <li>PostgreSQL EXCLUDE constraint on {@code (vehicle_category, unit, effective_period)} prevents overlapping rules</li>
 *   <li>Attempts to save overlapping rules will throw {@code DataIntegrityViolationException}</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRule
 * @see com.services.pricing_rules_service.service.PricingRuleService
 */
@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    /**
     * Finds all active pricing rules.
     * 
     * <p>Returns only rules where {@code active = true}, regardless of their effective period.
     * This method is primarily used for cache warm-up and admin dashboards.</p>
     * 
     * @return List of all active rules, ordered by vehicle category and unit
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.active = true ORDER BY pr.vehicleCategory, pr.unit")
    List<PricingRule> findAllActiveRules();

    /**
     * Finds the active pricing rule applicable at a specific timestamp for a given vehicle category and pricing unit.
     * 
     * <p>This is the primary query method for pricing calculations. It returns the rule where:</p>
     * <ul>
     *   <li>{@code active = true}</li>
     *   <li>{@code vehicleCategory} matches the requested category</li>
     *   <li>{@code unit} matches the requested unit (MINUTE, HOUR, or DAY)</li>
     *   <li>{@code effectiveFrom <= timestamp < effectiveTo} (or effectiveTo is NULL)</li>
     * </ul>
     * 
     * <p><strong>Cache Strategy:</strong> Results are cached with key {@code pricingRules::{category}::{unit}}.
     * The service layer handles caching via {@code @Cacheable}.</p>
     * 
     * <p><strong>Business Rule:</strong> The EXCLUDE constraint ensures at most one rule can match these criteria,
     * so this method returns {@code Optional} rather than a list.</p>
     * 
     * <p><strong>Usage Example:</strong></p>
     * <pre>{@code
     * Optional<PricingRule> rule = repository.findActiveRule(
     *     VehicleCategory.STANDARD, 
     *     PricingUnit.HOUR, 
     *     Instant.now()
     * );
     * }</pre>
     * 
     * @param category The vehicle category (ECONOM, STANDARD, PREMIUM)
     * @param unit The pricing unit (MINUTE, HOUR, DAY)
     * @param timestamp The timestamp to check against the effective period
     * @return Optional containing the matching rule, or empty if no rule applies
     */
    @Query("""
        SELECT pr FROM PricingRule pr 
        WHERE pr.active = true 
          AND pr.vehicleCategory = :category 
          AND pr.unit = :unit 
          AND pr.effectiveFrom <= :timestamp 
          AND (pr.effectiveTo IS NULL OR pr.effectiveTo > :timestamp)
        """)
    Optional<PricingRule> findActiveRule(
        @Param("category") VehicleCategory category,
        @Param("unit") PricingUnit unit,
        @Param("timestamp") Instant timestamp
    );

    /**
     * Finds all rules that have expired as of the given timestamp.
     * 
     * <p>This method is used by the {@code DeactivateExpiredRulesJob} scheduled job
     * to identify rules where {@code effectiveTo < now} but {@code active = true}.</p>
     * 
     * <p>Such rules should be deactivated to prevent accidental use and to optimize
     * cache performance (by excluding stale rules from cache warm-up).</p>
     * 
     * <p><strong>Scheduled Execution:</strong> Runs daily at 02:00 via Quartz.</p>
     * 
     * @param now The current timestamp to compare against effectiveTo
     * @return List of rules that are active but have passed their effectiveTo date
     */
    @Query("""
        SELECT pr FROM PricingRule pr 
        WHERE pr.active = true 
          AND pr.effectiveTo IS NOT NULL 
          AND pr.effectiveTo < :now
        """)
    List<PricingRule> findExpiredRules(@Param("now") Instant now);

    /**
     * Finds all active rules for a specific vehicle category.
     * 
     * <p>Returns all active rules (MINUTE, HOUR, DAY) for a given category,
     * regardless of their effective period. Useful for admin interfaces showing
     * all pricing tiers for a category.</p>
     * 
     * @param category The vehicle category to filter by
     * @return List of active rules for the specified category, ordered by unit
     */
    @Query("SELECT pr FROM PricingRule pr WHERE pr.active = true AND pr.vehicleCategory = :category ORDER BY pr.unit")
    List<PricingRule> findActiveRulesByCategory(@Param("category") VehicleCategory category);

    /**
     * Finds all active rules for a specific pricing unit across all categories.
     * 
     * <p>Returns all active rules for a given unit (e.g., all HOUR-based rules),
     * regardless of category. Useful for comparative pricing analysis.</p>
     * 
     * @param unit The pricing unit to filter by
     * @return List of active rules for the specified unit, ordered by category and price
     */
    @Query("""
        SELECT pr FROM PricingRule pr 
        WHERE pr.active = true AND pr.unit = :unit 
        ORDER BY pr.vehicleCategory, pr.pricePerUnit
        """)
    List<PricingRule> findActiveRulesByUnit(@Param("unit") PricingUnit unit);

    /**
     * Checks if any active rule exists for a given category and unit at a specific timestamp.
     * 
     * <p>Used for validation before creating new rules to provide user-friendly error messages
     * about existing conflicting rules (in addition to the database EXCLUDE constraint).</p>
     * 
     * @param category The vehicle category
     * @param unit The pricing unit
     * @param timestamp The timestamp to check
     * @return {@code true} if a rule exists, {@code false} otherwise
     */
    @Query("""
        SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END 
        FROM PricingRule pr 
        WHERE pr.active = true 
          AND pr.vehicleCategory = :category 
          AND pr.unit = :unit 
          AND pr.effectiveFrom <= :timestamp 
          AND (pr.effectiveTo IS NULL OR pr.effectiveTo > :timestamp)
        """)
    boolean existsActiveRule(
        @Param("category") VehicleCategory category,
        @Param("unit") PricingUnit unit,
        @Param("timestamp") Instant timestamp
    );

    /**
     * Counts the total number of active pricing rules.
     * 
     * <p>Useful for metrics and monitoring dashboards.</p>
     * 
     * @return Count of rules where {@code active = true}
     */
    @Query("SELECT COUNT(pr) FROM PricingRule pr WHERE pr.active = true")
    long countActiveRules();
}
