package com.services.car_service.service;

import com.services.car_service.domain.entity.Car;
import com.services.car_service.domain.enums.VehicleCategory;
import com.services.car_service.domain.repository.CarRepository;
import com.services.car_service.dto.CarResponse;
import com.services.car_service.dto.CreateCarRequest;
import com.services.car_service.dto.UpdateCarRequest;
import com.services.car_service.exception.BusinessException;
import com.services.car_service.exception.ResourceNotFoundException;
import com.services.car_service.exception.ValidationException;
import com.services.car_service.mapper.CarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Business logic service for Car management.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>CRUD operations with owner-based access control</li>
 *   <li>Advanced search and filtering</li>
 *   <li>Cache management (Redis)</li>
 *   <li>Validation (uniqueness, ownership)</li>
 * </ul>
 * 
 * <p>Caching strategy:
 * <ul>
 *   <li>{@code publicCars} - cached public listings</li>
 *   <li>{@code carDetails} - cached individual car lookups</li>
 *   <li>{@code ownerCars} - cached owner-specific lists</li>
 * </ul>
 * 
 * <p>Cache invalidation:
 * <ul>
 *   <li>Create/update/delete → evict all caches</li>
 *   <li>Shareable/archived change → evict publicCars</li>
 * </ul>
 * 
 * @see Car
 * @see CarRepository
 * @see CarMapper
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CarService {

    private final CarRepository carRepository;
    private final CarMapper carMapper;

    /**
     * Retrieves a car by ID with caching.
     *
     * @param id the car ID
     * @return the car response DTO
     * @throws ResourceNotFoundException if car not found
     */
    @Cacheable(value = "carDetails", key = "#id")
    public CarResponse getCarById(Long id) {
        log.debug("Fetching car by ID: {}", id);
        Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));
        return carMapper.toResponse(car);
    }

    /**
     * Retrieves all public cars (shareable and not archived) with caching.
     * 
     * <p>Supports pagination and sorting.
     *
     * @param pageable pagination and sorting information
     * @return a page of public cars
     */
    @Cacheable(value = "publicCars", key = "#pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getPublicCars(Pageable pageable) {
        log.debug("Fetching public cars with pagination: {}", pageable);
        return carRepository.findPublicCars(pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Retrieves public cars filtered by brand.
     *
     * @param brand the brand to filter by
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Cacheable(value = "publicCars", key = "'brand-' + #brand + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getPublicCarsByBrand(String brand, Pageable pageable) {
        log.debug("Fetching public cars by brand: {}", brand);
        return carRepository.findPublicCarsByBrand(brand, pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Retrieves public cars filtered by category.
     *
     * @param category the vehicle category
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Cacheable(value = "publicCars", key = "'category-' + #category + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getPublicCarsByCategory(VehicleCategory category, Pageable pageable) {
        log.debug("Fetching public cars by category: {}", category);
        return carRepository.findPublicCarsByCategory(category, pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Retrieves public cars within a price range.
     *
     * @param minPrice minimum daily price (inclusive)
     * @param maxPrice maximum daily price (inclusive)
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Cacheable(value = "publicCars", key = "'price-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getPublicCarsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Fetching public cars by price range: {} - {}", minPrice, maxPrice);
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("Minimum price cannot be greater than maximum price");
        }
        return carRepository.findPublicCarsByPriceRange(minPrice, maxPrice, pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Retrieves public cars with combined filters.
     *
     * @param brand optional brand filter (null to ignore)
     * @param category optional category filter (null to ignore)
     * @param minPrice minimum daily price
     * @param maxPrice maximum daily price
     * @param pageable pagination and sorting information
     * @return a page of matching cars
     */
    @Cacheable(value = "publicCars", key = "'filters-' + #brand + '-' + #category + '-' + #minPrice + '-' + #maxPrice + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getPublicCarsWithFilters(String brand, VehicleCategory category, 
                                                       BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.debug("Fetching public cars with filters - brand: {}, category: {}, price: {} - {}", 
                  brand, category, minPrice, maxPrice);
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new ValidationException("Minimum price cannot be greater than maximum price");
        }
        return carRepository.findPublicCarsWithFilters(brand, category, minPrice, maxPrice, pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Retrieves all cars owned by a specific account.
     *
     * @param ownerId the owner account ID
     * @param pageable pagination information
     * @return a page of owner's cars
     */
    @Cacheable(value = "ownerCars", key = "#ownerId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Page<CarResponse> getCarsByOwner(String ownerId, Pageable pageable) {
        log.debug("Fetching cars for owner: {}", ownerId);
        return carRepository.findByOwnerId(ownerId, pageable)
            .map(carMapper::toResponse);
    }

    /**
     * Creates a new car (owner-based access).
     * 
     * <p>Validates:
     * <ul>
     *   <li>Registration number uniqueness (case-insensitive)</li>
     *   <li>Owner ID from authenticated user</li>
     * </ul>
     *
     * @param request the creation request
     * @param ownerId the owner account ID (from JWT)
     * @return the created car response
     * @throws ValidationException if registration number already exists
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "publicCars", allEntries = true),
        @CacheEvict(value = "ownerCars", allEntries = true)
    })
    @PreAuthorize("isAuthenticated()")
    public CarResponse createCar(CreateCarRequest request, String ownerId) {
        log.info("Creating new car for owner: {}", ownerId);
        
        // Validate registration number uniqueness
        if (carRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new ValidationException("Registration number already exists: " + request.getRegistrationNumber());
        }

        Car car = carMapper.toEntity(request);
        car.setOwnerId(ownerId);
        
        Car savedCar = carRepository.save(car);
        log.info("Car created successfully with ID: {}", savedCar.getId());
        
        return carMapper.toResponse(savedCar);
    }

    /**
     * Updates an existing car (owner-only access).
     * 
     * <p>Validates:
     * <ul>
     *   <li>Car exists</li>
     *   <li>Requester is the owner</li>
     *   <li>Registration number uniqueness (if changed)</li>
     * </ul>
     *
     * @param id the car ID
     * @param request the update request
     * @param requesterId the requester account ID (from JWT)
     * @return the updated car response
     * @throws ResourceNotFoundException if car not found
     * @throws BusinessException if requester is not the owner
     * @throws ValidationException if new registration number exists
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "carDetails", key = "#id"),
        @CacheEvict(value = "publicCars", allEntries = true),
        @CacheEvict(value = "ownerCars", allEntries = true)
    })
    @PreAuthorize("isAuthenticated()")
    public CarResponse updateCar(Long id, UpdateCarRequest request, String requesterId) {
        log.info("Updating car {} by user: {}", id, requesterId);
        
        Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));

        // Owner-based access control
        if (!car.isOwnedBy(requesterId)) {
            throw new BusinessException("You are not authorized to update this car");
        }

        // Validate registration number uniqueness (if changed)
        if (request.getRegistrationNumber() != null && 
            !request.getRegistrationNumber().equalsIgnoreCase(car.getRegistrationNumber())) {
            if (carRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
                throw new ValidationException("Registration number already exists: " + request.getRegistrationNumber());
            }
        }

        carMapper.updateCarFromRequest(request, car);
        
        Car updatedCar = carRepository.save(car);
        log.info("Car {} updated successfully", id);
        
        return carMapper.toResponse(updatedCar);
    }

    /**
     * Deletes a car (owner-only access, soft delete via archived flag).
     *
     * @param id the car ID
     * @param requesterId the requester account ID (from JWT)
     * @throws ResourceNotFoundException if car not found
     * @throws BusinessException if requester is not the owner
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "carDetails", key = "#id"),
        @CacheEvict(value = "publicCars", allEntries = true),
        @CacheEvict(value = "ownerCars", allEntries = true)
    })
    @PreAuthorize("isAuthenticated()")
    public void deleteCar(Long id, String requesterId) {
        log.info("Deleting (archiving) car {} by user: {}", id, requesterId);
        
        Car car = carRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Car not found with id: " + id));

        // Owner-based access control
        if (!car.isOwnedBy(requesterId)) {
            throw new BusinessException("You are not authorized to delete this car");
        }

        // Soft delete: mark as archived
        car.setArchived(true);
        car.setShareable(false); // Also hide from public listings
        carRepository.save(car);
        
        log.info("Car {} archived successfully", id);
    }

    /**
     * Checks if a registration number is available (case-insensitive).
     *
     * @param registrationNumber the registration number to check
     * @return true if available, false otherwise
     */
    public boolean isRegistrationNumberAvailable(String registrationNumber) {
        return !carRepository.existsByRegistrationNumber(registrationNumber);
    }

    /**
     * Counts publicly visible cars.
     *
     * @return the total count
     */
    public long countPublicCars() {
        return carRepository.countPublicCars();
    }

    /**
     * Counts cars owned by a specific account.
     *
     * @param ownerId the owner account ID
     * @return the count
     */
    public long countCarsByOwner(String ownerId) {
        return carRepository.countByOwnerId(ownerId);
    }
}
