package com.services.car_service.domain.entity;

import com.services.car_service.domain.enums.FuelType;
import com.services.car_service.domain.enums.TransmissionType;
import com.services.car_service.domain.enums.VehicleCategory;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a vehicle in the car-sharing fleet.
 * 
 * <p>Maps to table {@code car.cars} in PostgreSQL with the following key features:
 * <ul>
 *   <li>Case-insensitive unique registration number (citext)</li>
 *   <li>Owner-based access control (owner_id foreign key)</li>
 *   <li>Shareable flag for public listing visibility</li>
 *   <li>Archived flag for soft deletion</li>
 *   <li>Full audit trail (created/modified timestamps and actors)</li>
 * </ul>
 * 
 * <p>Constraints:
 * <ul>
 *   <li>Registration number must be unique (case-insensitive)</li>
 *   <li>Daily price must be non-negative</li>
 *   <li>Number of seats must be positive</li>
 * </ul>
 * 
 * @see VehicleCategory
 * @see TransmissionType
 * @see FuelType
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Entity
@Table(name = "cars", schema = "car", indexes = {
    @Index(name = "idx_cars_owner_id", columnList = "owner_id"),
    @Index(name = "idx_cars_brand_price", columnList = "brand, daily_price"),
    @Index(name = "idx_cars_category_price", columnList = "category, daily_price")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ownerId"}) // Avoid potential lazy-loading issues
public class Car implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key (auto-generated).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Vehicle brand (e.g., "Toyota", "BMW").
     */
    @NotBlank(message = "Brand is required")
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String brand;

    /**
     * Vehicle model (e.g., "Corolla", "X5").
     */
    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String model;

    /**
     * Registration number (license plate) - case-insensitive unique.
     * 
     * <p>Uses PostgreSQL citext type for case-insensitive uniqueness.
     */
    @NotBlank(message = "Registration number is required")
    @Column(name = "registration_number", nullable = false, unique = true, columnDefinition = "citext")
    private String registrationNumber;

    /**
     * Detailed vehicle description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * URL to vehicle image.
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * Number of passenger seats.
     */
    @NotNull(message = "Number of seats is required")
    @Positive(message = "Number of seats must be positive")
    @Column(nullable = false)
    @Builder.Default
    private Short seats = 5;

    /**
     * Transmission type (MANUAL or AUTOMATIC).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type", columnDefinition = "transmission_type")
    private TransmissionType transmissionType;

    /**
     * Fuel type (GASOLINE, DIESEL, ELECTRIC, HYBRID, PLUG_IN_HYBRID).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", columnDefinition = "fuel_type")
    private FuelType fuelType;

    /**
     * Vehicle category for pricing tier (ECONOM, STANDARD, PREMIUM).
     */
    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "vehicle_category")
    private VehicleCategory category;

    /**
     * Daily rental price (base rate).
     */
    @NotNull(message = "Daily price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Daily price must be non-negative")
    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    /**
     * Owner (operator) account ID.
     * 
     * <p>References {@code identity.accounts.id} with ON DELETE RESTRICT.
     */
    @NotBlank(message = "Owner ID is required")
    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    /**
     * Archived flag for soft deletion.
     * 
     * <p>Archived cars are excluded from public listings.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean archived = false;

    /**
     * Shareable flag for public listing visibility.
     * 
     * <p>Only shareable cars appear in public searches.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean shareable = true;

    // === Audit fields ===

    /**
     * Timestamp when the car was created.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private Instant createdDate;

    /**
     * Timestamp when the car was last modified.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

    /**
     * Account ID of the user who created the car.
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 255)
    private String createdBy;

    /**
     * Account ID of the user who last modified the car.
     */
    @LastModifiedBy
    @Column(name = "last_modified_by", length = 255)
    private String lastModifiedBy;

    // === Business methods ===

    /**
     * Checks if the car is publicly visible (shareable and not archived).
     *
     * @return true if the car can be listed publicly
     */
    public boolean isPubliclyVisible() {
        return Boolean.TRUE.equals(shareable) && Boolean.FALSE.equals(archived);
    }

    /**
     * Checks if a given account owns this car.
     *
     * @param accountId the account ID to check
     * @return true if the account is the owner
     */
    public boolean isOwnedBy(String accountId) {
        return ownerId != null && ownerId.equals(accountId);
    }
}
