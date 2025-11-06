package com.services.pricing_rules_service.domain.enums;

/**
 * Enum representing the time unit for pricing calculations in the Car Sharing system.
 * 
 * <p>This enum defines the granularity at which rental pricing can be configured and calculated.
 * Each pricing rule specifies a {@code price_per_unit} for one of these time intervals.</p>
 * 
 * <p><strong>Business Rules:</strong></p>
 * <ul>
 *   <li>Multiple rules can exist for different units (e.g., MINUTE, HOUR, DAY) for the same vehicle category</li>
 *   <li>The pricing calculation engine selects the most cost-effective unit combination for the total rental duration</li>
 *   <li>Temporal constraints (EXCLUDE on effective_period) prevent overlapping rules for the same (vehicle_category, unit)</li>
 * </ul>
 * 
 * <p><strong>Database Mapping:</strong></p>
 * <ul>
 *   <li>PostgreSQL enum type: {@code pricing_unit}</li>
 *   <li>Column: {@code pricing.pricing_rule.unit}</li>
 *   <li>Index: {@code idx_pricing_rule_lookup(vehicle_category, unit, active)}</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Creating a pricing rule for hourly rentals
 * PricingRule rule = PricingRule.builder()
 *     .unit(PricingUnit.HOUR)
 *     .vehicleCategory(VehicleCategory.STANDARD)
 *     .pricePerUnit(new BigDecimal("15.00"))
 *     .build();
 * 
 * // Calculating cost for a 3-hour rental
 * BigDecimal cost = pricePerUnit.multiply(BigDecimal.valueOf(3)); // 45.00
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.domain.entity.PricingRule
 */
public enum PricingUnit {
    /**
     * Pricing per minute.
     * 
     * <p>Used for short-term rentals (e.g., quick errands, city trips).
     * Typical duration range: 1 minute to 59 minutes.</p>
     * 
     * <p><strong>Example:</strong> €0.30/minute → 30 minutes = €9.00</p>
     */
    MINUTE,
    
    /**
     * Pricing per hour.
     * 
     * <p>Most common unit for mid-term rentals (e.g., half-day trips, errands).
     * Typical duration range: 1 hour to 23 hours.</p>
     * 
     * <p><strong>Example:</strong> €12.00/hour → 5 hours = €60.00</p>
     */
    HOUR,
    
    /**
     * Pricing per day (24-hour period).
     * 
     * <p>Used for long-term rentals (e.g., weekend trips, vacations).
     * Typical duration range: 1 day or more.</p>
     * 
     * <p><strong>Example:</strong> €80.00/day → 3 days = €240.00</p>
     */
    DAY
}
