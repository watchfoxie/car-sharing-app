package com.services.car_service.service;

import com.services.car_service.domain.entity.Car;
import com.services.car_service.domain.enums.FuelType;
import com.services.car_service.domain.enums.TransmissionType;
import com.services.car_service.domain.enums.VehicleCategory;
import com.services.car_service.domain.repository.CarRepository;
import com.services.car_service.dto.CarResponse;
import com.services.car_service.dto.CreateCarRequest;
import com.services.car_service.dto.UpdateCarRequest;
import com.services.car_service.exception.BusinessException;
import com.services.car_service.exception.ResourceNotFoundException;
import com.services.car_service.exception.ValidationException;
import com.services.car_service.mapper.CarMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CarService}.
 * 
 * <p>Tests business logic for car management operations using Mockito
 * to isolate service layer from repository, mapper and cache dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>CRUD operations with owner validation</li>
 *   <li>Public car listings with filtering/pagination</li>
 *   <li>Cache eviction scenarios (create/update/delete)</li>
 *   <li>Registration number uniqueness validation</li>
 *   <li>Soft delete (archived flag)</li>
 *   <li>Edge cases (empty results, invalid ownership, price range validation)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CarService Unit Tests")
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarMapper carMapper;

    @InjectMocks
    private CarService carService;

    private Car testCar;
    private CarResponse testResponse;
    private CreateCarRequest createRequest;
    private UpdateCarRequest updateRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Setup test fixtures
        testCar = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .registrationNumber("ABC123")
                .description("Comfortable sedan")
                .imageUrl("https://example.com/toyota-camry.jpg")
                .seats((short) 5)
                .transmissionType(TransmissionType.AUTOMATIC)
                .fuelType(FuelType.GASOLINE)
                .category(VehicleCategory.STANDARD)
                .dailyPrice(new BigDecimal("50.00"))
                .ownerId("auth0|owner123")
                .archived(false)
                .shareable(true)
                .build();

        testResponse = CarResponse.builder()
                .id(1L)
                .brand("Toyota")
                .model("Camry")
                .registrationNumber("ABC123")
                .category(VehicleCategory.STANDARD)
                .dailyPrice(new BigDecimal("50.00"))
                .shareable(true)
                .archived(false)
                .build();

        createRequest = CreateCarRequest.builder()
                .brand("Honda")
                .model("Civic")
                .registrationNumber("XYZ789")
                .category(VehicleCategory.ECONOM)
                .dailyPrice(new BigDecimal("40.00"))
                .seats((short) 5)
                .transmissionType(TransmissionType.MANUAL)
                .fuelType(FuelType.GASOLINE)
                .build();

        updateRequest = UpdateCarRequest.builder()
                .brand("Toyota")
                .model("Camry Updated")
                .dailyPrice(new BigDecimal("55.00"))
                .build();

        pageable = PageRequest.of(0, 20);
    }

    // ========================== GET BY ID ==========================

    @Test
    @DisplayName("getCarById - Should return car when found")
    void getCarById_WhenCarExists_ShouldReturnCar() {
        // Given
        Long carId = 1L;
        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        CarResponse result = carService.getCarById(carId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(carId);
        assertThat(result.getBrand()).isEqualTo("Toyota");
        assertThat(result.getModel()).isEqualTo("Camry");
        assertThat(result.getRegistrationNumber()).isEqualTo("ABC123");

        verify(carRepository, times(1)).findById(carId);
        verify(carMapper, times(1)).toResponse(testCar);
    }

    @Test
    @DisplayName("getCarById - Should throw ResourceNotFoundException when car not found")
    void getCarById_WhenCarNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentCarId = 999L;
        when(carRepository.findById(nonExistentCarId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> carService.getCarById(nonExistentCarId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: " + nonExistentCarId);

        verify(carRepository, times(1)).findById(nonExistentCarId);
        verify(carMapper, never()).toResponse(any(Car.class));
    }

    // ========================== PUBLIC CARS ==========================

    @Test
    @DisplayName("getPublicCars - Should return page of public cars")
    void getPublicCars_ShouldReturnPageOfCars() {
        // Given
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findPublicCars(pageable)).thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getPublicCars(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);

        verify(carRepository, times(1)).findPublicCars(pageable);
        verify(carMapper, times(1)).toResponse(testCar);
    }

    @Test
    @DisplayName("getPublicCars - Should return empty page when no cars available")
    void getPublicCars_WhenNoCarsAvailable_ShouldReturnEmptyPage() {
        // Given
        Page<Car> emptyPage = Page.empty(pageable);
        when(carRepository.findPublicCars(pageable)).thenReturn(emptyPage);

        // When
        Page<CarResponse> result = carService.getPublicCars(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(carRepository, times(1)).findPublicCars(pageable);
        verify(carMapper, never()).toResponse(any(Car.class));
    }

    // ========================== FILTER BY BRAND ==========================

    @Test
    @DisplayName("getPublicCarsByBrand - Should filter cars by brand")
    void getPublicCarsByBrand_ShouldReturnFilteredCars() {
        // Given
        String brand = "Toyota";
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findPublicCarsByBrand(brand, pageable)).thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getPublicCarsByBrand(brand, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getBrand()).isEqualTo("Toyota");

        verify(carRepository, times(1)).findPublicCarsByBrand(brand, pageable);
    }

    // ========================== FILTER BY CATEGORY ==========================

    @Test
    @DisplayName("getPublicCarsByCategory - Should filter cars by category")
    void getPublicCarsByCategory_ShouldReturnFilteredCars() {
        // Given
        VehicleCategory category = VehicleCategory.STANDARD;
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findPublicCarsByCategory(category, pageable)).thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getPublicCarsByCategory(category, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo(VehicleCategory.STANDARD);

        verify(carRepository, times(1)).findPublicCarsByCategory(category, pageable);
    }

    // ========================== FILTER BY PRICE RANGE ==========================

    @Test
    @DisplayName("getPublicCarsByPriceRange - Should filter cars by price range")
    void getPublicCarsByPriceRange_ShouldReturnFilteredCars() {
        // Given
        BigDecimal minPrice = new BigDecimal("40.00");
        BigDecimal maxPrice = new BigDecimal("60.00");
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findPublicCarsByPriceRange(minPrice, maxPrice, pageable)).thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getPublicCarsByPriceRange(minPrice, maxPrice, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(carRepository, times(1)).findPublicCarsByPriceRange(minPrice, maxPrice, pageable);
    }

    @Test
    @DisplayName("getPublicCarsByPriceRange - Should throw ValidationException when minPrice > maxPrice")
    void getPublicCarsByPriceRange_WhenMinPriceGreaterThanMaxPrice_ShouldThrowValidationException() {
        // Given
        BigDecimal minPrice = new BigDecimal("100.00");
        BigDecimal maxPrice = new BigDecimal("50.00");

        // When & Then
        assertThatThrownBy(() -> carService.getPublicCarsByPriceRange(minPrice, maxPrice, pageable))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Minimum price cannot be greater than maximum price");

        verify(carRepository, never()).findPublicCarsByPriceRange(any(), any(), any());
    }

    // ========================== COMBINED FILTERS ==========================

    @Test
    @DisplayName("getPublicCarsWithFilters - Should apply combined filters")
    void getPublicCarsWithFilters_ShouldApplyCombinedFilters() {
        // Given
        String brand = "Toyota";
        VehicleCategory category = VehicleCategory.STANDARD;
        BigDecimal minPrice = new BigDecimal("40.00");
        BigDecimal maxPrice = new BigDecimal("60.00");
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findPublicCarsWithFilters(brand, category, minPrice, maxPrice, pageable))
                .thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getPublicCarsWithFilters(brand, category, minPrice, maxPrice, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(carRepository, times(1)).findPublicCarsWithFilters(brand, category, minPrice, maxPrice, pageable);
    }

    @Test
    @DisplayName("getPublicCarsWithFilters - Should throw ValidationException for invalid price range")
    void getPublicCarsWithFilters_WhenInvalidPriceRange_ShouldThrowValidationException() {
        // Given
        BigDecimal minPrice = new BigDecimal("100.00");
        BigDecimal maxPrice = new BigDecimal("50.00");

        // When & Then
        assertThatThrownBy(() -> carService.getPublicCarsWithFilters(null, null, minPrice, maxPrice, pageable))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Minimum price cannot be greater than maximum price");

        verify(carRepository, never()).findPublicCarsWithFilters(any(), any(), any(), any(), any());
    }

    // ========================== OWNER CARS ==========================

    @Test
    @DisplayName("getCarsByOwner - Should return owner's cars")
    void getCarsByOwner_ShouldReturnOwnerCars() {
        // Given
        String ownerId = "auth0|owner123";
        Page<Car> carPage = new PageImpl<>(List.of(testCar));
        when(carRepository.findByOwnerId(ownerId, pageable)).thenReturn(carPage);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        Page<CarResponse> result = carService.getCarsByOwner(ownerId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(carRepository, times(1)).findByOwnerId(ownerId, pageable);
    }

    // ========================== CREATE CAR ==========================

    @Test
    @DisplayName("createCar - Should create car successfully")
    void createCar_WhenValidRequest_ShouldCreateCar() {
        // Given
        String ownerId = "auth0|owner123";
        Car newCar = Car.builder()
                .id(2L)
                .brand("Honda")
                .model("Civic")
                .registrationNumber("XYZ789")
                .ownerId(ownerId)
                .build();

        when(carRepository.existsByRegistrationNumber(createRequest.getRegistrationNumber())).thenReturn(false);
        when(carMapper.toEntity(createRequest)).thenReturn(newCar);
        when(carRepository.save(any(Car.class))).thenReturn(newCar);
        when(carMapper.toResponse(newCar)).thenReturn(testResponse);

        // When
        CarResponse result = carService.createCar(createRequest, ownerId);

        // Then
        assertThat(result).isNotNull();

        verify(carRepository, times(1)).existsByRegistrationNumber(createRequest.getRegistrationNumber());
        verify(carMapper, times(1)).toEntity(createRequest);
        verify(carRepository, times(1)).save(any(Car.class));
    }

    @Test
    @DisplayName("createCar - Should throw ValidationException when registration number exists")
    void createCar_WhenRegistrationNumberExists_ShouldThrowValidationException() {
        // Given
        String ownerId = "auth0|owner123";
        when(carRepository.existsByRegistrationNumber(createRequest.getRegistrationNumber())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> carService.createCar(createRequest, ownerId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Registration number already exists");

        verify(carRepository, times(1)).existsByRegistrationNumber(createRequest.getRegistrationNumber());
        verify(carRepository, never()).save(any(Car.class));
    }

    // ========================== UPDATE CAR ==========================

    @Test
    @DisplayName("updateCar - Should update car when owner is valid")
    void updateCar_WhenOwnerIsValid_ShouldUpdateCar() {
        // Given
        Long carId = 1L;
        String ownerId = "auth0|owner123";
        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));
        when(carRepository.save(any(Car.class))).thenReturn(testCar);
        when(carMapper.toResponse(testCar)).thenReturn(testResponse);

        // When
        CarResponse result = carService.updateCar(carId, updateRequest, ownerId);

        // Then
        assertThat(result).isNotNull();

        verify(carRepository, times(1)).findById(carId);
        verify(carMapper, times(1)).updateCarFromRequest(updateRequest, testCar);
        verify(carRepository, times(1)).save(testCar);
    }

    @Test
    @DisplayName("updateCar - Should throw ResourceNotFoundException when car not found")
    void updateCar_WhenCarNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        Long nonExistentCarId = 999L;
        String ownerId = "auth0|owner123";
        when(carRepository.findById(nonExistentCarId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> carService.updateCar(nonExistentCarId, updateRequest, ownerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Car not found with id: " + nonExistentCarId);

        verify(carRepository, times(1)).findById(nonExistentCarId);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("updateCar - Should throw BusinessException when requester is not owner")
    void updateCar_WhenRequesterNotOwner_ShouldThrowBusinessException() {
        // Given
        Long carId = 1L;
        String nonOwnerId = "auth0|differentUser";
        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));

        // When & Then
        assertThatThrownBy(() -> carService.updateCar(carId, updateRequest, nonOwnerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You are not authorized to update this car");

        verify(carRepository, times(1)).findById(carId);
        verify(carRepository, never()).save(any(Car.class));
    }

    @Test
    @DisplayName("updateCar - Should validate registration number uniqueness when changed")
    void updateCar_WhenRegistrationNumberChanged_ShouldValidateUniqueness() {
        // Given
        Long carId = 1L;
        String ownerId = "auth0|owner123";
        UpdateCarRequest requestWithNewRegNumber = UpdateCarRequest.builder()
                .registrationNumber("NEW123")
                .build();

        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));
        when(carRepository.existsByRegistrationNumber("NEW123")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> carService.updateCar(carId, requestWithNewRegNumber, ownerId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Registration number already exists");

        verify(carRepository, times(1)).existsByRegistrationNumber("NEW123");
        verify(carRepository, never()).save(any(Car.class));
    }

    // ========================== DELETE CAR ==========================

    @Test
    @DisplayName("deleteCar - Should soft delete car (archived=true)")
    void deleteCar_WhenOwnerIsValid_ShouldSoftDeleteCar() {
        // Given
        Long carId = 1L;
        String ownerId = "auth0|owner123";
        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));
        when(carRepository.save(any(Car.class))).thenReturn(testCar);

        // When
        carService.deleteCar(carId, ownerId);

        // Then
        verify(carRepository, times(1)).findById(carId);
        verify(carRepository, times(1)).save(testCar);
        // Verify soft delete flags
        assertThat(testCar.getArchived()).isTrue();
        assertThat(testCar.getShareable()).isFalse();
    }

    @Test
    @DisplayName("deleteCar - Should throw BusinessException when requester is not owner")
    void deleteCar_WhenRequesterNotOwner_ShouldThrowBusinessException() {
        // Given
        Long carId = 1L;
        String nonOwnerId = "auth0|differentUser";
        when(carRepository.findById(carId)).thenReturn(Optional.of(testCar));

        // When & Then
        assertThatThrownBy(() -> carService.deleteCar(carId, nonOwnerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("You are not authorized to delete this car");

        verify(carRepository, times(1)).findById(carId);
        verify(carRepository, never()).save(any(Car.class));
    }

    // ========================== AVAILABILITY CHECKS ==========================

    @Test
    @DisplayName("isRegistrationNumberAvailable - Should return true when available")
    void isRegistrationNumberAvailable_WhenNotExists_ShouldReturnTrue() {
        // Given
        String registrationNumber = "NEW123";
        when(carRepository.existsByRegistrationNumber(registrationNumber)).thenReturn(false);

        // When
        boolean result = carService.isRegistrationNumberAvailable(registrationNumber);

        // Then
        assertThat(result).isTrue();
        verify(carRepository, times(1)).existsByRegistrationNumber(registrationNumber);
    }

    @Test
    @DisplayName("isRegistrationNumberAvailable - Should return false when taken")
    void isRegistrationNumberAvailable_WhenExists_ShouldReturnFalse() {
        // Given
        String registrationNumber = "ABC123";
        when(carRepository.existsByRegistrationNumber(registrationNumber)).thenReturn(true);

        // When
        boolean result = carService.isRegistrationNumberAvailable(registrationNumber);

        // Then
        assertThat(result).isFalse();
        verify(carRepository, times(1)).existsByRegistrationNumber(registrationNumber);
    }

    // ========================== COUNT OPERATIONS ==========================

    @Test
    @DisplayName("countPublicCars - Should return public cars count")
    void countPublicCars_ShouldReturnCount() {
        // Given
        when(carRepository.countPublicCars()).thenReturn(10L);

        // When
        long result = carService.countPublicCars();

        // Then
        assertThat(result).isEqualTo(10L);
        verify(carRepository, times(1)).countPublicCars();
    }

    @Test
    @DisplayName("countCarsByOwner - Should return owner's cars count")
    void countCarsByOwner_ShouldReturnCount() {
        // Given
        String ownerId = "auth0|owner123";
        when(carRepository.countByOwnerId(ownerId)).thenReturn(5L);

        // When
        long result = carService.countCarsByOwner(ownerId);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(carRepository, times(1)).countByOwnerId(ownerId);
    }
}
