package com.services.car_service.dto;

import com.services.car_service.domain.enums.FuelType;
import com.services.car_service.domain.enums.TransmissionType;
import com.services.car_service.domain.enums.VehicleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for car information exposed via API.
 * 
 * <p>Includes all public car details plus ownership and operational flags.
 * This DTO is cacheable and optimized for listing operations.
 * 
 * <p>Fields:
 * <ul>
 *   <li>Basic info: id, brand, model, registrationNumber</li>
 *   <li>Specifications: seats, transmission, fuel type, category</li>
 *   <li>Media: description, imageUrl</li>
 *   <li>Pricing: dailyPrice, avgRating (aggregated from feedback-service)</li>
 *   <li>Operational: shareable, archived, ownerId (for owner validation)</li>
 *   <li>Audit: createdDate, lastModifiedDate</li>
 * </ul>
 * 
 * @see com.services.car_service.domain.entity.Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique car identifier.
     */
    private Long id;

    /**
     * Vehicle brand.
     */
    private String brand;

    /**
     * Vehicle model.
     */
    private String model;

    /**
     * Registration number (license plate).
     */
    private String registrationNumber;

    /**
     * Detailed vehicle description.
     */
    private String description;

    /**
     * URL to vehicle image.
     */
    private String imageUrl;

    /**
     * Number of passenger seats.
     */
    private Short seats;

    /**
     * Transmission type (MANUAL or AUTOMATIC).
     */
    private TransmissionType transmissionType;

    /**
     * Fuel type.
     */
    private FuelType fuelType;

    /**
     * Vehicle category for pricing tier.
     */
    private VehicleCategory category;

    /**
     * Daily rental price.
     */
    private BigDecimal dailyPrice;

    /**
     * Owner (operator) account ID.
     */
    private String ownerId;

    /**
     * Archived flag.
     */
    private Boolean archived;

    /**
     * Shareable flag for public listing.
     */
    private Boolean shareable;

    /**
     * Average rating from feedback (aggregated).
     * 
     * <p>Populated by calling feedback-service or consuming feedback events.
     * Null if no ratings exist.
     */
    private Double avgRating;

    /**
     * Timestamp when the car was created.
     */
    private Instant createdDate;

    /**
     * Timestamp when the car was last modified.
     */
    private Instant lastModifiedDate;
}
