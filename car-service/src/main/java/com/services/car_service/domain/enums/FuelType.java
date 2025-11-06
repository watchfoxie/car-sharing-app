package com.services.car_service.domain.enums;

/**
 * Fuel type enumeration for vehicles.
 * 
 * <p>Supported fuel types covering traditional and modern propulsion:
 * <ul>
 *   <li>{@code GASOLINE} - Traditional petrol engines</li>
 *   <li>{@code DIESEL} - Diesel engines</li>
 *   <li>{@code ELECTRIC} - Fully electric vehicles (EV)</li>
 *   <li>{@code HYBRID} - Combined combustion and electric (HEV)</li>
 *   <li>{@code PLUG_IN_HYBRID} - Rechargeable hybrid (PHEV)</li>
 * </ul>
 * 
 * <p>This enum is synchronized with the PostgreSQL type {@code fuel_type}
 * defined in the database schema.
 * 
 * @see com.services.car_service.domain.entity.Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public enum FuelType {
    /**
     * Gasoline (petrol) engine.
     */
    GASOLINE,
    
    /**
     * Diesel engine.
     */
    DIESEL,
    
    /**
     * Fully electric vehicle.
     */
    ELECTRIC,
    
    /**
     * Hybrid electric vehicle (non-rechargeable).
     */
    HYBRID,
    
    /**
     * Plug-in hybrid electric vehicle (rechargeable).
     */
    PLUG_IN_HYBRID
}
