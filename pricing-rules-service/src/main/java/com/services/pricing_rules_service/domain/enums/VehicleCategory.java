package com.services.pricing_rules_service.domain.enums;

/**
 * Enum representing vehicle categories for pricing and classification in the Car Sharing system.
 * 
 * <p>This enum categorizes vehicles into tiers that determine pricing, features, and target customer segments.
 * Each category has distinct characteristics in terms of vehicle quality, amenities, and rental rates.</p>
 * 
 * <p><strong>Business Rules:</strong></p>
 * <ul>
 *   <li>Pricing rules are defined per vehicle category (e.g., ECONOM has lower rates than PREMIUM)</li>
 *   <li>Categories are synchronized with {@code car.cars.category} in the Car Service</li>
 *   <li>Multiple pricing rules (MINUTE/HOUR/DAY) can exist for each category</li>
 * </ul>
 * 
 * <p><strong>Database Mapping:</strong></p>
 * <ul>
 *   <li>PostgreSQL enum type: {@code vehicle_category} (shared across services)</li>
 *   <li>Column: {@code pricing.pricing_rule.vehicle_category}</li>
 *   <li>Synchronized with: {@code car.cars.category}</li>
 *   <li>Index: {@code idx_pricing_rule_lookup(vehicle_category, unit, active)}</li>
 * </ul>
 * 
 * <p><strong>Typical Characteristics:</strong></p>
 * <table border="1">
 *   <tr>
 *     <th>Category</th>
 *     <th>Vehicle Examples</th>
 *     <th>Features</th>
 *     <th>Target Segment</th>
 *     <th>Price Range</th>
 *   </tr>
 *   <tr>
 *     <td>ECONOM</td>
 *     <td>Dacia Sandero, Renault Clio</td>
 *     <td>Basic, fuel-efficient</td>
 *     <td>Budget-conscious, city trips</td>
 *     <td>€30-50/day</td>
 *   </tr>
 *   <tr>
 *     <td>STANDARD</td>
 *     <td>VW Golf, Toyota Corolla</td>
 *     <td>Comfortable, reliable</td>
 *     <td>General use, families</td>
 *     <td>€50-80/day</td>
 *   </tr>
 *   <tr>
 *     <td>PREMIUM</td>
 *     <td>BMW 3 Series, Audi A4</td>
 *     <td>Luxury, advanced features</td>
 *     <td>Business, special occasions</td>
 *     <td>€80-150/day</td>
 *   </tr>
 * </table>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Creating pricing rules for different categories
 * PricingRule economRule = PricingRule.builder()
 *     .vehicleCategory(VehicleCategory.ECONOM)
 *     .unit(PricingUnit.DAY)
 *     .pricePerUnit(new BigDecimal("35.00"))
 *     .build();
 * 
 * PricingRule premiumRule = PricingRule.builder()
 *     .vehicleCategory(VehicleCategory.PREMIUM)
 *     .unit(PricingUnit.DAY)
 *     .pricePerUnit(new BigDecimal("120.00"))
 *     .build();
 * }</pre>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see com.services.pricing_rules_service.domain.entity.PricingRule
 */
public enum VehicleCategory {
    /**
     * Economy category - budget-friendly vehicles.
     * 
     * <p>Basic features, fuel-efficient, ideal for short city trips and cost-conscious renters.
     * Typically includes compact cars with manual transmission.</p>
     * 
     * <p><strong>Target use cases:</strong></p>
     * <ul>
     *   <li>Daily errands and short commutes</li>
     *   <li>Budget travelers</li>
     *   <li>Single riders or small groups (2-3 passengers)</li>
     * </ul>
     */
    ECONOM,
    
    /**
     * Standard category - mid-range vehicles.
     * 
     * <p>Comfortable, reliable, suitable for families and longer trips.
     * Typically includes mid-size sedans or hatchbacks with automatic/manual options.</p>
     * 
     * <p><strong>Target use cases:</strong></p>
     * <ul>
     *   <li>Family outings and weekend trips</li>
     *   <li>Business travel</li>
     *   <li>Medium-distance journeys (up to 500 km)</li>
     * </ul>
     */
    STANDARD,
    
    /**
     * Premium category - luxury vehicles.
     * 
     * <p>High-end features, superior comfort, advanced technology.
     * Typically includes executive sedans, luxury SUVs, or performance cars.</p>
     * 
     * <p><strong>Target use cases:</strong></p>
     * <ul>
     *   <li>Corporate executives and business meetings</li>
     *   <li>Special occasions (weddings, celebrations)</li>
     *   <li>Clients seeking premium driving experience</li>
     * </ul>
     */
    PREMIUM
}
