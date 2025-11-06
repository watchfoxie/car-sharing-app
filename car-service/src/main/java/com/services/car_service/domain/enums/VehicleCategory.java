package com.services.car_service.domain.enums;

/**
 * Vehicle category enumeration aligned with pricing tiers.
 * 
 * <p>Categories determine the base pricing structure and target customer segment:
 * <ul>
 *   <li>{@code ECONOM} - Budget-friendly vehicles with basic features</li>
 *   <li>{@code STANDARD} - Mid-range vehicles with balanced comfort and price</li>
 *   <li>{@code PREMIUM} - High-end vehicles with luxury features</li>
 * </ul>
 * 
 * <p>This enum is synchronized with the PostgreSQL type {@code vehicle_category}
 * defined in the database schema.
 * 
 * @see com.services.car_service.domain.entity.Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public enum VehicleCategory {
    /**
     * Economy class vehicles - budget-friendly option.
     */
    ECONOM,
    
    /**
     * Standard class vehicles - balanced comfort and price.
     */
    STANDARD,
    
    /**
     * Premium class vehicles - luxury and high-end features.
     */
    PREMIUM
}
