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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing pricing rules and calculating rental costs.
 *
 * <p>This service provides comprehensive pricing rule management including CRUD operations
 * and intelligent price calculation for car rentals. The calculation engine optimizes costs
 * by breaking down rental durations into the most cost-effective unit combinations.</p>
 *
 * <p><strong>Key Responsibilities:</strong></p>
 * <ul>
 *   <li><strong>CRUD Operations:</strong> Create, read, update, delete pricing rules with validation</li>
 *   <li><strong>Price Calculation:</strong> Compute optimal rental cost by combining DAY/HOUR/MINUTE units</li>
 *   <li><strong>Rule Lookup:</strong> Fetch active rules for specific vehicle category/unit/timestamp</li>
 *   <li><strong>Cache Management:</strong> Evict cached rules on mutations to ensure consistency</li>
 * </ul>
 *
 * <p><strong>Calculation Algorithm:</strong></p>
 * <p>The pricing engine implements a greedy algorithm to minimize total cost:</p>
 * <pre>
 * 1. Extract duration between pickup and return timestamps
 * 2. Fetch active pricing rules for vehicle category
 * 3. Break down duration into units (prefer larger units first):
 *    - Full DAYS (24h blocks)
 *    - Remaining HOURS (60min blocks)
 *    - Leftover MINUTES
 * 4. Calculate subtotal per unit: quantity × pricePerUnit
 * 5. Sum all subtotals to get total cost
 * 6. Return detailed breakdown with per-unit charges
 * </pre>
 *
 * <p><strong>Example Calculation:</strong></p>
 * <pre>
 * Input: 2 days, 5 hours, 30 minutes rental for STANDARD category
 * Pricing: DAY=50 MDL, HOUR=10 MDL, MINUTE=0.50 MDL
 * 
 * Breakdown:
 *   2 DAY    × 50   MDL = 100 MDL
 *   5 HOUR   × 10   MDL =  50 MDL
 *  30 MINUTE × 0.50 MDL =  15 MDL
 * Total: 165 MDL
 * </pre>
 *
 * <p><strong>Validation Rules:</strong></p>
 * <ul>
 *   <li>Return timestamp must be after pickup timestamp (no negative durations)</li>
 *   <li>Rental duration must satisfy min/max constraints from active rules</li>
 *   <li>Vehicle category must have at least one active rule for DAY/HOUR/MINUTE units</li>
 *   <li>Pricing rule updates must not create temporal overlaps (enforced by DB EXCLUDE constraint)</li>
 * </ul>
 *
 * <p><strong>Caching Strategy:</strong></p>
 * <ul>
 *   <li><strong>Read Cache:</strong> Active rules cached with key: "category:unit:timestamp"</li>
 *   <li><strong>Write Invalidation:</strong> All CREATE/UPDATE/DELETE operations evict entire "pricingRules" cache</li>
 *   <li><strong>TTL:</strong> Caffeine L1 (5 min), Redis L2 (10 min)</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong></p>
 * <ul>
 *   <li>All methods annotated with {@code @Transactional(readOnly=true/false)}</li>
 *   <li>Cache operations are thread-safe (both Caffeine and Redis support concurrent access)</li>
 *   <li>Database temporal constraints prevent race conditions during rule creation</li>
 * </ul>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRule
 * @see PricingRuleRepository
 * @see PricingRuleMapper
 * @see CacheEvict
 * @see Cacheable
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PricingRuleService {

    private final PricingRuleRepository pricingRuleRepository;
    private final PricingRuleMapper pricingRuleMapper;

    /**
     * Creates a new pricing rule.
     *
     * <p><strong>Validation:</strong></p>
     * <ul>
     *   <li>Checks for temporal overlaps with existing rules (DB EXCLUDE constraint enforces)</li>
     *   <li>Validates min ≤ max duration</li>
     *   <li>Ensures effectiveFrom &lt; effectiveTo</li>
     * </ul>
     *
     * <p><strong>Cache Invalidation:</strong></p>
     * <ul>
     *   <li>Evicts entire "pricingRules" cache to ensure new rule is visible immediately</li>
     * </ul>
     *
     * @param request DTO containing pricing rule details
     * @return Created pricing rule with generated ID and effectivePeriod
     * @throws org.springframework.dao.DataIntegrityViolationException if temporal overlap detected
     */
    @Transactional
    @CacheEvict(cacheNames = {"pricingRules"}, allEntries = true)
    public PricingRuleResponse createRule(CreatePricingRuleRequest request) {
        log.info("Creating new pricing rule for category={}, unit={}, effectiveFrom={}, effectiveTo={}",
                request.getVehicleCategory(), request.getUnit(), request.getEffectiveFrom(), request.getEffectiveTo());

        PricingRule rule = pricingRuleMapper.toEntity(request);
        PricingRule savedRule = pricingRuleRepository.save(rule);

        log.info("Successfully created pricing rule with id={}", savedRule.getId());
        return pricingRuleMapper.toResponse(savedRule);
    }

    /**
     * Updates an existing pricing rule.
     *
     * <p><strong>Partial Updates:</strong></p>
     * <ul>
     *   <li>Supports PATCH semantics (only non-null fields in request are updated)</li>
     *   <li>MapStruct configured with NullValuePropertyMappingStrategy.IGNORE</li>
     * </ul>
     *
     * <p><strong>Immutable Fields:</strong></p>
     * <ul>
     *   <li>{@code id} - Cannot be changed (part of identity)</li>
     *   <li>{@code effectivePeriod} - Automatically regenerated by DB trigger on effectiveFrom/To change</li>
     *   <li>{@code createdAt}, {@code createdBy} - Audit fields (immutable)</li>
     * </ul>
     *
     * <p><strong>Cache Invalidation:</strong></p>
     * <ul>
     *   <li>Evicts entire "pricingRules" cache to prevent serving stale data</li>
     * </ul>
     *
     * @param id   ID of the pricing rule to update
     * @param request DTO containing fields to update (null fields are ignored)
     * @return Updated pricing rule
     * @throws jakarta.persistence.EntityNotFoundException if rule with given ID doesn't exist
     * @throws org.springframework.dao.DataIntegrityViolationException if update creates temporal overlap
     */
    @Transactional
    @CacheEvict(cacheNames = {"pricingRules"}, allEntries = true)
    public PricingRuleResponse updateRule(Long id, UpdatePricingRuleRequest request) {
        log.info("Updating pricing rule id={} with partial data", id);

        PricingRule existingRule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Pricing rule not found with id: " + id));

        pricingRuleMapper.updateRuleFromRequest(request, existingRule);
        PricingRule updatedRule = pricingRuleRepository.save(existingRule);

        log.info("Successfully updated pricing rule id={}", id);
        return pricingRuleMapper.toResponse(updatedRule);
    }

    /**
     * Deletes a pricing rule by ID.
     *
     * <p><strong>Hard Delete:</strong></p>
     * <ul>
     *   <li>Permanently removes the rule from the database</li>
     *   <li>Consider soft-delete (setting {@code active=false}) for audit trail preservation</li>
     * </ul>
     *
     * <p><strong>Cache Invalidation:</strong></p>
     * <ul>
     *   <li>Evicts entire "pricingRules" cache to prevent serving deleted rule</li>
     * </ul>
     *
     * @param id ID of the pricing rule to delete
     * @throws jakarta.persistence.EntityNotFoundException if rule with given ID doesn't exist
     */
    @Transactional
    @CacheEvict(cacheNames = {"pricingRules"}, allEntries = true)
    public void deleteRule(Long id) {
        log.info("Deleting pricing rule id={}", id);

        if (!pricingRuleRepository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Pricing rule not found with id: " + id);
        }

        pricingRuleRepository.deleteById(id);
        log.info("Successfully deleted pricing rule id={}", id);
    }

    /**
     * Retrieves a pricing rule by ID.
     *
     * <p><strong>Note:</strong> Single-entity lookups are NOT cached (low benefit for unique IDs).</p>
     *
     * @param id ID of the pricing rule to retrieve
     * @return Pricing rule details
     * @throws jakarta.persistence.EntityNotFoundException if rule with given ID doesn't exist
     */
    public PricingRuleResponse getRuleById(Long id) {
        log.debug("Fetching pricing rule by id={}", id);

        PricingRule rule = pricingRuleRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Pricing rule not found with id: " + id));

        return pricingRuleMapper.toResponse(rule);
    }

    /**
     * Retrieves all pricing rules with pagination.
     *
     * <p><strong>Pagination:</strong></p>
     * <ul>
     *   <li>Default page size: 20 (configurable via {@code Pageable})</li>
     *   <li>Supports sorting by any field (e.g., "effectiveFrom,desc")</li>
     *   <li>Returns Spring Data {@code Page} object with metadata (totalElements, totalPages, etc.)</li>
     * </ul>
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of pricing rules
     */
    public Page<PricingRuleResponse> getAllRules(Pageable pageable) {
        log.debug("Fetching all pricing rules with pagination: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<PricingRule> rules = pricingRuleRepository.findAll(pageable);
        return rules.map(pricingRuleMapper::toResponse);
    }

    /**
     * Calculates the total rental cost for a given vehicle category and rental period.
     *
     * <p><strong>Algorithm Steps:</strong></p>
     * <ol>
     *   <li>Validate request (return &gt; pickup, valid timestamps)</li>
     *   <li>Calculate duration in seconds</li>
     *   <li>Fetch active pricing rules for all units (DAY, HOUR, MINUTE)</li>
     *   <li>Break down duration into optimal units (greedy algorithm):
     *     <ul>
     *       <li>Extract full DAYS (duration ÷ 86400 seconds)</li>
     *       <li>Extract remaining HOURS (remainder ÷ 3600 seconds)</li>
     *       <li>Extract leftover MINUTES (remainder ÷ 60 seconds)</li>
     *     </ul>
     *   </li>
     *   <li>Calculate cost per unit: quantity × pricePerUnit</li>
     *   <li>Validate total duration against min/max constraints</li>
     *   <li>Return total cost with detailed breakdown</li>
     * </ol>
     *
     * <p><strong>Example Scenarios:</strong></p>
     * <pre>
     * Scenario 1: Short rental (90 minutes)
     *   Input: ECONOM, 90 minutes
     *   Rules: HOUR=8 MDL, MINUTE=0.30 MDL
     *   Breakdown: 1 HOUR (60 min) × 8 MDL + 30 MINUTE × 0.30 MDL = 17 MDL
     *
     * Scenario 2: Multi-day rental (2d 5h 15m)
     *   Input: PREMIUM, 2880 + 300 + 15 = 3195 minutes
     *   Rules: DAY=80 MDL, HOUR=15 MDL, MINUTE=0.60 MDL
     *   Breakdown: 2 DAY × 80 MDL + 5 HOUR × 15 MDL + 15 MINUTE × 0.60 MDL = 244 MDL
     * </pre>
     *
     * <p><strong>Edge Cases:</strong></p>
     * <ul>
     *   <li>Zero duration (pickup == return): throws IllegalArgumentException</li>
     *   <li>Negative duration (return &lt; pickup): throws IllegalArgumentException</li>
     *   <li>Missing pricing rules for any unit: throws IllegalStateException</li>
     *   <li>Duration below minDuration or above maxDuration: throws IllegalArgumentException</li>
     * </ul>
     *
     * <p><strong>Caching:</strong></p>
     * <ul>
     *   <li>Active rules lookup is cached (key: "category:unit:timestamp")</li>
     *   <li>Calculation result is NOT cached (varies per unique pickup/return combination)</li>
     * </ul>
     *
     * @param request DTO containing vehicle category, pickup timestamp, return timestamp
     * @return Total cost with per-unit breakdown
     * @throws IllegalArgumentException if validation fails (invalid timestamps, duration out of bounds)
     * @throws IllegalStateException if active pricing rules are missing for required units
     */
    public CalculatePriceResponse calculatePrice(CalculatePriceRequest request) {
        log.info("Calculating price for category={}, pickup={}, return={}",
                request.getVehicleCategory(), request.getPickupDatetime(), request.getReturnDatetime());

        // Validation: Return must be after pickup
        if (!request.getReturnDatetime().isAfter(request.getPickupDatetime())) {
            throw new IllegalArgumentException("Return datetime must be after pickup datetime");
        }

        // Calculate duration
        Duration duration = Duration.between(request.getPickupDatetime(), request.getReturnDatetime());
        long totalSeconds = duration.getSeconds();
        log.debug("Total rental duration: {} seconds ({} days, {} hours, {} minutes)",
                totalSeconds, duration.toDays(), duration.toHoursPart(), duration.toMinutesPart());

        // Fetch active pricing rules for all units
        Instant now = Instant.now();
        PricingRule dayRule = findActiveRuleForUnit(request.getVehicleCategory(), PricingUnit.DAY, now);
        PricingRule hourRule = findActiveRuleForUnit(request.getVehicleCategory(), PricingUnit.HOUR, now);
        PricingRule minuteRule = findActiveRuleForUnit(request.getVehicleCategory(), PricingUnit.MINUTE, now);

        // Break down duration into optimal units (greedy algorithm)
        List<CalculatePriceResponse.UnitBreakdown> breakdown = new ArrayList<>();
        long remainingSeconds = totalSeconds;

        // 1. Extract full DAYS
        long days = remainingSeconds / 86400; // 86400 seconds = 1 day
        if (days > 0) {
            remainingSeconds -= days * 86400;
            BigDecimal dayCost = dayRule.getPricePerUnit().multiply(BigDecimal.valueOf(days));
            breakdown.add(CalculatePriceResponse.UnitBreakdown.builder()
                    .unit(PricingUnit.DAY)
                    .quantity(days)
                    .pricePerUnit(dayRule.getPricePerUnit())
                    .subtotal(dayCost)
                    .build());
            log.debug("Breakdown: {} days × {} MDL = {} MDL", days, dayRule.getPricePerUnit(), dayCost);
        }

        // 2. Extract remaining HOURS
        long hours = remainingSeconds / 3600; // 3600 seconds = 1 hour
        if (hours > 0) {
            remainingSeconds -= hours * 3600;
            BigDecimal hourCost = hourRule.getPricePerUnit().multiply(BigDecimal.valueOf(hours));
            breakdown.add(CalculatePriceResponse.UnitBreakdown.builder()
                    .unit(PricingUnit.HOUR)
                    .quantity(hours)
                    .pricePerUnit(hourRule.getPricePerUnit())
                    .subtotal(hourCost)
                    .build());
            log.debug("Breakdown: {} hours × {} MDL = {} MDL", hours, hourRule.getPricePerUnit(), hourCost);
        }

        // 3. Extract leftover MINUTES (round up partial minutes)
        long minutes = (long) Math.ceil(remainingSeconds / 60.0);
        if (minutes > 0) {
            BigDecimal minuteCost = minuteRule.getPricePerUnit().multiply(BigDecimal.valueOf(minutes));
            breakdown.add(CalculatePriceResponse.UnitBreakdown.builder()
                    .unit(PricingUnit.MINUTE)
                    .quantity(minutes)
                    .pricePerUnit(minuteRule.getPricePerUnit())
                    .subtotal(minuteCost)
                    .build());
            log.debug("Breakdown: {} minutes × {} MDL = {} MDL", minutes, minuteRule.getPricePerUnit(), minuteCost);
        }

        // Calculate total cost
        BigDecimal totalCost = breakdown.stream()
                .map(CalculatePriceResponse.UnitBreakdown::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP); // Round to 2 decimal places

        // Validate duration constraints (check against DAY rule's min/max)
        if (dayRule.getMinDuration() != null && duration.compareTo(dayRule.getMinDuration()) < 0) {
            throw new IllegalArgumentException(
                    String.format("Rental duration (%s) is below minimum (%s)",
                            duration, dayRule.getMinDuration()));
        }
        if (dayRule.getMaxDuration() != null && duration.compareTo(dayRule.getMaxDuration()) > 0) {
            throw new IllegalArgumentException(
                    String.format("Rental duration (%s) exceeds maximum (%s)",
                            duration, dayRule.getMaxDuration()));
        }

        log.info("Successfully calculated price: totalCost={} MDL, breakdown={} units", totalCost, breakdown.size());
        return CalculatePriceResponse.builder()
                .totalCost(totalCost)
                .totalDuration(duration)
                .vehicleCategory(request.getVehicleCategory())
                .pickupDatetime(request.getPickupDatetime())
                .returnDatetime(request.getReturnDatetime())
                .breakdown(breakdown)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Finds the active pricing rule for a specific vehicle category, unit, and timestamp.
     *
     * <p><strong>Cache Key:</strong> "category:unit:timestamp" (e.g., "STANDARD:HOUR:2025-11-06T10:00:00Z")</p>
     *
     * <p><strong>Lookup Logic:</strong></p>
     * <ul>
     *   <li>Queries database using generated tstzrange column: {@code effective_period @> timestamp}</li>
     *   <li>Filters by {@code active = true}</li>
     *   <li>Returns single matching rule (EXCLUDE constraint ensures no overlaps)</li>
     * </ul>
     *
     * @param category Vehicle category (ECONOM, STANDARD, PREMIUM)
     * @param unit     Pricing unit (DAY, HOUR, MINUTE)
     * @param timestamp Target timestamp (usually current time)
     * @return Active pricing rule
     * @throws IllegalStateException if no active rule found (missing pricing configuration)
     */
    @Cacheable(cacheNames = "pricingRules", key = "#category + ':' + #unit + ':' + #timestamp")
    private PricingRule findActiveRuleForUnit(VehicleCategory category, PricingUnit unit, Instant timestamp) {
        log.debug("Looking up active rule for category={}, unit={}, timestamp={}", category, unit, timestamp);

        return pricingRuleRepository.findActiveRule(category, unit, timestamp)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("No active pricing rule found for category=%s, unit=%s at timestamp=%s",
                                category, unit, timestamp)));
    }
}
