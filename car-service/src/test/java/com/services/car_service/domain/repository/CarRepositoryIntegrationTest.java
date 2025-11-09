package com.services.car_service.domain.repository;

import com.services.car_service.domain.entity.Car;
import com.services.car_service.domain.enums.FuelType;
import com.services.car_service.domain.enums.TransmissionType;
import com.services.car_service.domain.enums.VehicleCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CarRepository using Testcontainers.
 * 
 * <p>Tests:
 * <ul>
 *   <li>CRUD operations</li>
 *   <li>Case-insensitive registration number uniqueness (citext)</li>
 *   <li>Public car filtering (shareable, archived)</li>
 *   <li>Filtering by brand, category, price range</li>
 *   <li>Sorting (brand A-Z/Z-A, price ascending/descending)</li>
 *   <li>Pagination</li>
 *   <li>Audit fields population</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
class CarRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("car_sharing_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CarRepository carRepository;

    private Car testCar1;
    private Car testCar2;
    private Car testCar3;

    @BeforeEach
    void setUp() {
        carRepository.deleteAll();

        // Car 1: Economy, shareable, not archived
        testCar1 = Car.builder()
            .brand("Toyota")
            .model("Corolla")
            .registrationNumber("ABC123")
            .description("Reliable economy car")
            .seats((short) 5)
            .transmissionType(TransmissionType.AUTOMATIC)
            .fuelType(FuelType.GASOLINE)
            .category(VehicleCategory.ECONOM)
            .dailyPrice(new BigDecimal("50.00"))
            .ownerId("owner1")
            .shareable(true)
            .archived(false)
            .build();

        // Car 2: Premium, shareable, not archived
        testCar2 = Car.builder()
            .brand("BMW")
            .model("X5")
            .registrationNumber("XYZ789")
            .description("Luxury SUV")
            .seats((short) 7)
            .transmissionType(TransmissionType.AUTOMATIC)
            .fuelType(FuelType.DIESEL)
            .category(VehicleCategory.PREMIUM)
            .dailyPrice(new BigDecimal("150.00"))
            .ownerId("owner1")
            .shareable(true)
            .archived(false)
            .build();

        // Car 3: Standard, NOT shareable (should be excluded from public listings)
        testCar3 = Car.builder()
            .brand("Honda")
            .model("Civic")
            .registrationNumber("DEF456")
            .description("Standard sedan")
            .seats((short) 5)
            .transmissionType(TransmissionType.MANUAL)
            .fuelType(FuelType.GASOLINE)
            .category(VehicleCategory.STANDARD)
            .dailyPrice(new BigDecimal("75.00"))
            .ownerId("owner2")
            .shareable(false)
            .archived(false)
            .build();

        carRepository.save(testCar1);
        carRepository.save(testCar2);
        carRepository.save(testCar3);
    }

    @Test
    void testSaveAndFindById() {
        Optional<Car> found = carRepository.findById(testCar1.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getBrand()).isEqualTo("Toyota");
        assertThat(found.get().getRegistrationNumber()).isEqualTo("ABC123");
    }

    @Test
    void testFindByRegistrationNumber_CaseInsensitive() {
        // Test case-insensitive lookup (citext)
        Optional<Car> foundLower = carRepository.findByRegistrationNumber("abc123");
        Optional<Car> foundUpper = carRepository.findByRegistrationNumber("ABC123");
        Optional<Car> foundMixed = carRepository.findByRegistrationNumber("AbC123");

        assertThat(foundLower).isPresent();
        assertThat(foundUpper).isPresent();
        assertThat(foundMixed).isPresent();
        
        assertThat(foundLower.get().getId()).isEqualTo(testCar1.getId());
        assertThat(foundUpper.get().getId()).isEqualTo(testCar1.getId());
        assertThat(foundMixed.get().getId()).isEqualTo(testCar1.getId());
    }

    @Test
    void testExistsByRegistrationNumber_CaseInsensitive() {
        assertThat(carRepository.existsByRegistrationNumber("ABC123")).isTrue();
        assertThat(carRepository.existsByRegistrationNumber("abc123")).isTrue();
        assertThat(carRepository.existsByRegistrationNumber("AbC123")).isTrue();
        assertThat(carRepository.existsByRegistrationNumber("NOTEXIST")).isFalse();
    }

    @Test
    void testFindPublicCars() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> publicCars = carRepository.findPublicCars(pageable);

        // Only testCar1 and testCar2 should be included (shareable=true)
        // testCar3 is not shareable
        assertThat(publicCars.getTotalElements()).isEqualTo(2);
        assertThat(publicCars.getContent()).extracting("registrationNumber")
            .containsExactlyInAnyOrder("ABC123", "XYZ789");
    }

    @Test
    void testFindPublicCarsByBrand() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> toyotaCars = carRepository.findPublicCarsByBrand("Toyota", pageable);

        assertThat(toyotaCars.getTotalElements()).isEqualTo(1);
        assertThat(toyotaCars.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    void testFindPublicCarsByCategory() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> economCars = carRepository.findPublicCarsByCategory(VehicleCategory.ECONOM, pageable);

        assertThat(economCars.getTotalElements()).isEqualTo(1);
        assertThat(economCars.getContent().get(0).getCategory()).isEqualTo(VehicleCategory.ECONOM);
    }

    @Test
    void testFindPublicCarsByPriceRange() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> affordableCars = carRepository.findPublicCarsByPriceRange(
            new BigDecimal("40.00"), 
            new BigDecimal("100.00"), 
            pageable
        );

        // Only testCar1 (50.00) should match
        assertThat(affordableCars.getTotalElements()).isEqualTo(1);
        assertThat(affordableCars.getContent().get(0).getDailyPrice())
            .isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void testFindPublicCarsWithFilters() {
        PageRequest pageable = PageRequest.of(0, 10);
        
        // Filter by brand and price range
        Page<Car> filtered = carRepository.findPublicCarsWithFilters(
            "Toyota", 
            null, // no category filter
            new BigDecimal("0.00"), 
            new BigDecimal("100.00"), 
            pageable
        );

        assertThat(filtered.getTotalElements()).isEqualTo(1);
        assertThat(filtered.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    void testSortingByBrand_Ascending() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "brand"));
        Page<Car> sorted = carRepository.findPublicCars(pageable);

        assertThat(sorted.getContent()).extracting("brand")
            .containsExactly("BMW", "Toyota"); // Alphabetical A-Z
    }

    @Test
    void testSortingByBrand_Descending() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "brand"));
        Page<Car> sorted = carRepository.findPublicCars(pageable);

        assertThat(sorted.getContent()).extracting("brand")
            .containsExactly("Toyota", "BMW"); // Alphabetical Z-A
    }

    @Test
    void testSortingByPrice_Ascending() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "dailyPrice"));
        Page<Car> sorted = carRepository.findPublicCars(pageable);

        assertThat(sorted.getContent()).extracting("dailyPrice")
            .containsExactly(new BigDecimal("50.00"), new BigDecimal("150.00")); // Price ascending
    }

    @Test
    void testSortingByPrice_Descending() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dailyPrice"));
        Page<Car> sorted = carRepository.findPublicCars(pageable);

        assertThat(sorted.getContent()).extracting("dailyPrice")
            .containsExactly(new BigDecimal("150.00"), new BigDecimal("50.00")); // Price descending
    }

    @Test
    void testFindByOwnerId() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> owner1Cars = carRepository.findByOwnerId("owner1", pageable);

        // Owner1 has 2 cars (testCar1 and testCar2)
        assertThat(owner1Cars.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testCountPublicCars() {
        long count = carRepository.countPublicCars();
        
        // Only 2 public cars (testCar1, testCar2)
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testCountByOwnerId() {
        long count = carRepository.countByOwnerId("owner1");
        
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testPagination() {
        // First page (size 1)
        PageRequest firstPage = PageRequest.of(0, 1, Sort.by("brand"));
        Page<Car> page1 = carRepository.findPublicCars(firstPage);

        assertThat(page1.getTotalElements()).isEqualTo(2);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.getContent().get(0).getBrand()).isEqualTo("BMW");

        // Second page
        PageRequest secondPage = PageRequest.of(1, 1, Sort.by("brand"));
        Page<Car> page2 = carRepository.findPublicCars(secondPage);

        assertThat(page2.getContent()).hasSize(1);
        assertThat(page2.getContent().get(0).getBrand()).isEqualTo("Toyota");
    }

    @Test
    void testArchivedCarsExcludedFromPublicListings() {
        // Archive testCar1
        testCar1.setArchived(true);
        carRepository.save(testCar1);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Car> publicCars = carRepository.findPublicCars(pageable);

        // Only testCar2 should be visible (testCar1 archived, testCar3 not shareable)
        assertThat(publicCars.getTotalElements()).isEqualTo(1);
        assertThat(publicCars.getContent().get(0).getRegistrationNumber()).isEqualTo("XYZ789");
    }
}
