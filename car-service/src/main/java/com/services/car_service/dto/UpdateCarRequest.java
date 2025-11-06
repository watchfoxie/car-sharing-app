package com.services.car_service.dto;

import com.services.car_service.domain.enums.FuelType;
import com.services.car_service.domain.enums.TransmissionType;
import com.services.car_service.domain.enums.VehicleCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Request DTO for updating an existing car.
 * 
 * <p>All fields are optional (partial update supported).
 * Only the owner can update their own cars (validated in service layer).
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
public class UpdateCarRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Vehicle brand (optional).
     */
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    /**
     * Vehicle model (optional).
     */
    @Size(max = 100, message = "Model must not exceed 100 characters")
    private String model;

    /**
     * Registration number (optional, must be unique if provided).
     */
    @Pattern(regexp = "^[A-Za-z0-9-\\s]+$", 
             message = "Registration number can only contain alphanumeric characters, hyphens, and spaces")
    private String registrationNumber;

    /**
     * Detailed vehicle description (optional).
     */
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    /**
     * URL to vehicle image (optional).
     */
    @Size(max = 2048, message = "Image URL must not exceed 2048 characters")
    private String imageUrl;

    /**
     * Number of passenger seats (optional, positive if provided).
     */
    @Positive(message = "Number of seats must be positive")
    @Max(value = 100, message = "Number of seats must not exceed 100")
    private Short seats;

    /**
     * Transmission type (optional).
     */
    private TransmissionType transmissionType;

    /**
     * Fuel type (optional).
     */
    private FuelType fuelType;

    /**
     * Vehicle category (optional).
     */
    private VehicleCategory category;

    /**
     * Daily rental price (optional, non-negative if provided).
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily price must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Daily price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal dailyPrice;

    /**
     * Shareable flag (optional).
     */
    private Boolean shareable;

    /**
     * Archived flag (optional).
     */
    private Boolean archived;
}
