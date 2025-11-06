package com.services.car_service.domain.repository;

import com.services.car_service.domain.entity.Car;
import com.services.car_service.domain.enums.VehicleCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository interface for {@link Car} entity operations.
 * 
 * <p>Provides CRUD operations and custom query methods for:
 * <ul>
 *   <li>Owner-based filtering and access control</li>
 *   <li>Public listings (shareable and not archived)</li>
 *   <li>Advanced search (brand, category, price range)</li>
 *   <li>Case-insensitive registration number lookup</li>
 * </ul>
 * 
 * <p>Query methods leverage partial indexes defined in the database schema:
 * <ul>
 *   <li>{@code idx_cars_shareable_active} - for public listings</li>
 *   <li>{@code idx_cars_brand_price} - for brand/price sorting</li>
 *   <li>{@code idx_cars_category_price} - for category/price filtering</li>
 * </ul>
 * 
 * @see Car
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    /**
     * Finds a car by its registration number (case-insensitive).
     * 
     * <p>Leverages PostgreSQL citext type for case-insensitive matching.
     *
     * @param registrationNumber the registration number to search for
     * @return an Optional containing the car if found
     */
    Optional<Car> findByRegistrationNumber(String registrationNumber);

    /**
     * Checks if a car with the given registration number exists (case-insensitive).
     *
     * @param registrationNumber the registration number to check
     * @return true if a car exists with this registration number
     */
    boolean existsByRegistrationNumber(String registrationNumber);

    /**
     * Finds all cars owned by a specific account.
     *
     * @param ownerId the owner account ID
     * @param pageable pagination information
     * @return a page of cars owned by the account
     */
    Page<Car> findByOwnerId(String ownerId, Pageable pageable);

    /**
     * Finds all publicly visible cars (shareable and not archived).
     * 
     * <p>Uses partial index {@code idx_cars_shareable_active} for performance.
     *
     * @param pageable pagination and sorting information
     * @return a page of publicly visible cars
     */
    @Query("SELECT c FROM Car c WHERE c.shareable = true AND c.archived = false")
    Page<Car> findPublicCars(Pageable pageable);

    /**
     * Finds publicly visible cars filtered by brand.
     *
     * @param brand the brand to filter by
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Query("SELECT c FROM Car c WHERE c.shareable = true AND c.archived = false " +
           "AND LOWER(c.brand) = LOWER(:brand)")
    Page<Car> findPublicCarsByBrand(@Param("brand") String brand, Pageable pageable);

    /**
     * Finds publicly visible cars filtered by category.
     *
     * @param category the vehicle category to filter by
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Query("SELECT c FROM Car c WHERE c.shareable = true AND c.archived = false " +
           "AND c.category = :category")
    Page<Car> findPublicCarsByCategory(@Param("category") VehicleCategory category, Pageable pageable);

    /**
     * Finds publicly visible cars within a price range.
     *
     * @param minPrice minimum daily price (inclusive)
     * @param maxPrice maximum daily price (inclusive)
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Query("SELECT c FROM Car c WHERE c.shareable = true AND c.archived = false " +
           "AND c.dailyPrice BETWEEN :minPrice AND :maxPrice")
    Page<Car> findPublicCarsByPriceRange(@Param("minPrice") BigDecimal minPrice,
                                          @Param("maxPrice") BigDecimal maxPrice,
                                          Pageable pageable);

    /**
     * Finds publicly visible cars with combined filters.
     *
     * @param brand optional brand filter (null to ignore)
     * @param category optional category filter (null to ignore)
     * @param minPrice minimum daily price (inclusive)
     * @param maxPrice maximum daily price (inclusive)
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Query("SELECT c FROM Car c WHERE c.shareable = true AND c.archived = false " +
           "AND (:brand IS NULL OR LOWER(c.brand) = LOWER(:brand)) " +
           "AND (:category IS NULL OR c.category = :category) " +
           "AND c.dailyPrice BETWEEN :minPrice AND :maxPrice")
    Page<Car> findPublicCarsWithFilters(@Param("brand") String brand,
                                         @Param("category") VehicleCategory category,
                                         @Param("minPrice") BigDecimal minPrice,
                                         @Param("maxPrice") BigDecimal maxPrice,
                                         Pageable pageable);

    /**
     * Counts publicly visible cars.
     *
     * @return the total number of public cars
     */
    @Query("SELECT COUNT(c) FROM Car c WHERE c.shareable = true AND c.archived = false")
    long countPublicCars();

    /**
     * Counts cars owned by a specific account.
     *
     * @param ownerId the owner account ID
     * @return the number of cars owned by the account
     */
    long countByOwnerId(String ownerId);

    /**
     * Finds cars by owner and archived status.
     *
     * @param ownerId the owner account ID
     * @param archived the archived status to filter by
     * @param pageable pagination information
     * @return a page of matching cars
     */
    Page<Car> findByOwnerIdAndArchived(String ownerId, Boolean archived, Pageable pageable);
}
