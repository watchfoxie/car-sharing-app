package com.services.car_service.domain.enums;

/**
 * Transmission type enumeration for vehicles.
 * 
 * <p>Supported transmission types:
 * <ul>
 *   <li>{@code MANUAL} - Manual gearbox requiring driver gear shifting</li>
 *   <li>{@code AUTOMATIC} - Automatic transmission for easier driving</li>
 * </ul>
 * 
 * <p>This enum is synchronized with the PostgreSQL type {@code transmission_type}
 * defined in the database schema.
 * 
 * @see com.services.car_service.domain.entity.Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
public enum TransmissionType {
    /**
     * Manual transmission.
     */
    MANUAL,
    
    /**
     * Automatic transmission.
     */
    AUTOMATIC
}
