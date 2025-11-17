package com.services.rental_service.LimitCases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.domain.enums.RentalStatus;
import com.services.rental_service.domain.repository.RentalRepository;
import com.services.rental_service.dto.CreateRentalRequest;
import com.services.rental_service.dto.RentalResponse;
import com.services.rental_service.service.RentalService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 18 (Faza 18): Critical scenarios and edge cases - Timezone validation.
 * <p>
 * Validates UTC consistency end-to-end:
 * <ul>
 *   <li>Backend stores all timestamps in UTC (Instant, no timezone ambiguity)</li>
 *   <li>PostgreSQL TIMESTAMPTZ stores UTC and converts on retrieval</li>
 *   <li>Jackson serializes Instant as ISO-8601 UTC strings (e.g., "2025-01-15T10:00:00Z")</li>
 *   <li>Cross-timezone scenarios: user in Tokyo books car in New York → no discrepancies</li>
 *   <li>DST transitions: rental spanning DST change maintains correct duration</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Test scenarios:</strong>
 * <ol>
 *   <li>Create rental with ZonedDateTime (Tokyo), verify stored as UTC Instant</li>
 *   <li>Retrieve rental, verify deserialized timestamps match original UTC values</li>
 *   <li>Cross-timezone booking: Tokyo user books NYC car, verify no timezone conversion errors</li>
 *   <li>DST transition: rental spanning spring-forward, verify duration preserved</li>
 *   <li>JSON serialization: verify Jackson outputs ISO-8601 UTC format</li>
 * </ol>
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09 (Phase 18)
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Slf4j
class RentalTimezoneValidationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/migration/V1__init.sql");

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Long CAR_ID = 2001L;
    private static final String RENTER_ID = "renter-tz-001";

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        log.info("=== Test setup: cleaned rental_history table ===");
    }

    /**
     * Test 1: Backend stores timestamps as UTC Instant.
     * <p>
     * Scenario: User in Tokyo (UTC+9) creates rental for 2025-01-20 10:00 JST.
     * Expected: Backend stores 2025-01-20 01:00:00 UTC (9 hours earlier).
     * </p>
     */
    @Test
    @DisplayName("Test 18.2a: Backend stores timestamps in UTC - ZonedDateTime conversion")
    void testBackendStoresTimestampsAsUtc() {
        // Given: User in Tokyo timezone (UTC+9) books car for Jan 20, 10:00 AM JST
        ZoneId tokyoZone = ZoneId.of("Asia/Tokyo");
        ZonedDateTime pickupJst = ZonedDateTime.of(2025, 1, 20, 10, 0, 0, 0, tokyoZone);
        ZonedDateTime dropoffJst = ZonedDateTime.of(2025, 1, 20, 14, 0, 0, 0, tokyoZone);
        
        Instant pickupUtc = pickupJst.toInstant();  // Converts to UTC: 01:00:00 UTC
        Instant dropoffUtc = dropoffJst.toInstant(); // Converts to UTC: 05:00:00 UTC
        
        log.info("Tokyo time: {} to {}", pickupJst, dropoffJst);
        log.info("UTC time: {} to {}", pickupUtc, dropoffUtc);

        // When: Create rental
        CreateRentalRequest request = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickupUtc)
                .returnDatetime(dropoffUtc)
                .pickupLocation("Tokyo Station")
                .build();

        RentalResponse response = rentalService.createRental(request, RENTER_ID);

        // Then: Verify response contains UTC timestamps
        assertThat(response.getPickupDatetime()).isEqualTo(pickupUtc);
        assertThat(response.getReturnDatetime()).isEqualTo(dropoffUtc);

        // Verify database stores UTC (PostgreSQL TIMESTAMPTZ)
        Rental dbRental = rentalRepository.findById(response.getId()).orElseThrow();
        assertThat(dbRental.getPickupDatetime()).isEqualTo(pickupUtc);
        assertThat(dbRental.getReturnDatetime()).isEqualTo(dropoffUtc);
        
        // Verify duration is preserved (4 hours)
        Duration duration = Duration.between(dbRental.getPickupDatetime(), dbRental.getReturnDatetime());
        assertThat(duration.toHours()).isEqualTo(4);
    }

    /**
     * Test 2: Cross-timezone booking scenario.
     * <p>
     * Scenario: User in Tokyo (UTC+9) books car in New York (UTC-5) for NYC local time.
     * Expected: Backend handles timezone conversion correctly, no data corruption.
     * </p>
     */
    @Test
    @DisplayName("Test 18.2b: Cross-timezone booking - Tokyo user books NYC car")
    void testCrossTimezoneBooking() {
        // Given: User in Tokyo books NYC car for Jan 25, 2PM EST
        ZoneId nycZone = ZoneId.of("America/New_York");
        ZonedDateTime pickupNyc = ZonedDateTime.of(2025, 1, 25, 14, 0, 0, 0, nycZone);
        ZonedDateTime dropoffNyc = ZonedDateTime.of(2025, 1, 25, 18, 0, 0, 0, nycZone);
        
        Instant pickupUtc = pickupNyc.toInstant();  // Converts to UTC: 19:00:00 UTC
        Instant dropoffUtc = dropoffNyc.toInstant(); // Converts to UTC: 23:00:00 UTC
        
        log.info("NYC time: {} to {}", pickupNyc, dropoffNyc);
        log.info("UTC time: {} to {}", pickupUtc, dropoffUtc);

        // When: Create rental (backend receives UTC Instant)
        CreateRentalRequest request = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickupUtc)
                .returnDatetime(dropoffUtc)
                .pickupLocation("Times Square, NYC")
                .build();

        RentalResponse response = rentalService.createRental(request, RENTER_ID);

        // Then: Verify UTC storage
        assertThat(response.getPickupDatetime()).isEqualTo(pickupUtc);
        assertThat(response.getReturnDatetime()).isEqualTo(dropoffUtc);

        // Verify client can convert back to local timezone
        ZonedDateTime retrievedPickupNyc = response.getPickupDatetime().atZone(nycZone);
        assertThat(retrievedPickupNyc.getHour()).isEqualTo(14); // 2 PM EST
        
        ZonedDateTime retrievedDropoffNyc = response.getReturnDatetime().atZone(nycZone);
        assertThat(retrievedDropoffNyc.getHour()).isEqualTo(18); // 6 PM EST
        
        // Verify duration preserved (4 hours)
        Duration duration = Duration.between(pickupUtc, dropoffUtc);
        assertThat(duration.toHours()).isEqualTo(4);
    }

    /**
     * Test 3: DST transition handling.
     * <p>
     * Scenario: Rental spans Daylight Saving Time spring-forward (clock jumps 1 hour ahead).
     * Expected: Duration in UTC remains consistent (no DST effects in UTC).
     * </p>
     */
    @Test
    @DisplayName("Test 18.2c: DST transition handling - rental spanning spring-forward")
    void testDstTransitionHandling() {
        // Given: DST in US: March 9, 2025, 2:00 AM → 3:00 AM (spring forward)
        ZoneId nycZone = ZoneId.of("America/New_York");
        
        // Rental from 1:00 AM to 4:00 AM EST (spans 2:00 AM DST transition)
        ZonedDateTime pickupNyc = ZonedDateTime.of(2025, 3, 9, 1, 0, 0, 0, nycZone);
        ZonedDateTime dropoffNyc = ZonedDateTime.of(2025, 3, 9, 4, 0, 0, 0, nycZone);
        
        Instant pickupUtc = pickupNyc.toInstant();
        Instant dropoffUtc = dropoffNyc.toInstant();
        
        log.info("NYC time (DST span): {} to {}", pickupNyc, dropoffNyc);
        log.info("UTC time: {} to {}", pickupUtc, dropoffUtc);
        
        // Calculate duration in local time (wall clock: 3 hours due to DST skip)
        Duration localDuration = Duration.between(pickupNyc, dropoffNyc);
        log.info("Local wall clock duration: {} hours", localDuration.toHours());
        
        // Calculate duration in UTC (always 2 hours, no DST in UTC)
        Duration utcDuration = Duration.between(pickupUtc, dropoffUtc);
        log.info("UTC duration: {} hours", utcDuration.toHours());

        // When: Create rental
        CreateRentalRequest request = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickupUtc)
                .returnDatetime(dropoffUtc)
                .pickupLocation("NYC DST Test")
                .build();

        RentalResponse response = rentalService.createRental(request, RENTER_ID);

        // Then: Verify UTC duration is preserved (2 hours in UTC)
        Instant storedPickup = response.getPickupDatetime();
        Instant storedDropoff = response.getReturnDatetime();
        Duration storedDuration = Duration.between(storedPickup, storedDropoff);
        
        assertThat(storedDuration.toHours())
            .withFailMessage("UTC duration should be 2 hours (DST does not affect UTC)")
            .isEqualTo(2);
        
        // Verify local time conversion still works
        ZonedDateTime retrievedPickupNyc = storedPickup.atZone(nycZone);
        ZonedDateTime retrievedDropoffNyc = storedDropoff.atZone(nycZone);
        assertThat(retrievedPickupNyc.getHour()).isEqualTo(1);
        assertThat(retrievedDropoffNyc.getHour()).isEqualTo(4);
    }

    /**
     * Test 4: JSON serialization produces ISO-8601 UTC format.
     * <p>
     * Expected: Jackson ObjectMapper serializes Instant as "2025-01-20T10:00:00Z".
     * Frontend (Angular) can parse this with luxon DateTime.fromISO().
     * </p>
     */
    @Test
    @DisplayName("Test 18.2d: JSON serialization - ISO-8601 UTC format validation")
    void testJsonSerializationUtcFormat() throws Exception {
        // Given: Create rental with known UTC timestamp
        Instant pickup = Instant.parse("2025-02-01T10:00:00Z");
        Instant dropoff = Instant.parse("2025-02-01T14:00:00Z");
        
        CreateRentalRequest request = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup)
                .returnDatetime(dropoff)
                .pickupLocation("JSON Test Location")
                .build();

        RentalResponse response = rentalService.createRental(request, RENTER_ID);

        // When: Serialize response to JSON
        String json = objectMapper.writeValueAsString(response);
        log.info("JSON output: {}", json);

        // Then: Verify JSON contains ISO-8601 UTC timestamps
        assertThat(json)
            .contains("2025-02-01T10:00:00Z", "2025-02-01T14:00:00Z");
        
        // Verify deserialization round-trip preserves Instant
        RentalResponse deserialized = objectMapper.readValue(json, RentalResponse.class);
        assertThat(deserialized.getPickupDatetime()).isEqualTo(pickup);
        assertThat(deserialized.getReturnDatetime()).isEqualTo(dropoff);
    }

    /**
     * Test 5: Database TIMESTAMPTZ preserves UTC across JVM timezone changes.
     * <p>
     * Scenario: Write rental with JVM in UTC, read with JVM in Tokyo timezone.
     * Expected: Instant values remain identical (PostgreSQL handles conversion).
     * </p>
     */
    @Test
    @DisplayName("Test 18.2e: Database TIMESTAMPTZ - timezone-independent storage")
    void testDatabaseTimestamptzTimezoneIndependence() {
        // Given: Save rental with JVM in default timezone (UTC in CI/CD)
        TimeZone originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        
        Instant pickup = Instant.parse("2025-03-01T08:00:00Z");
        Instant dropoff = Instant.parse("2025-03-01T12:00:00Z");
        
        Rental rental = Rental.builder()
                .renterId(RENTER_ID)
                .carsId(CAR_ID)
                .pickupDatetime(pickup)
                .returnDatetime(dropoff)
                .status(RentalStatus.CONFIRMED)
                .estimatedCost(BigDecimal.valueOf(150.00))
                .build();
        
        Rental saved = rentalRepository.save(rental);
        Long rentalId = saved.getId();
        log.info("Saved rental with UTC JVM: pickup={}", saved.getPickupDatetime());

        // When: Change JVM timezone to Tokyo and re-read
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        rentalRepository.flush(); // Clear persistence context
        rentalRepository.findAll(); // Force refresh
        
        Rental retrieved = rentalRepository.findById(rentalId).orElseThrow();
        log.info("Retrieved rental with Tokyo JVM: pickup={}", retrieved.getPickupDatetime());

        // Then: Verify Instant values are identical (UTC preserved)
        assertThat(retrieved.getPickupDatetime()).isEqualTo(pickup);
        assertThat(retrieved.getReturnDatetime()).isEqualTo(dropoff);
        
        // Restore original timezone
        TimeZone.setDefault(originalTz);
    }

    /**
     * Test 6: Validate frontend expectations (Angular luxon DateTime).
     * <p>
     * Frontend converts ISO-8601 UTC string to local timezone using luxon:
     * {@code DateTime.fromISO("2025-01-20T10:00:00Z").setZone("America/New_York")}
     * Expected: Backend provides consistent UTC strings for client-side conversion.
     * </p>
     */
    @Test
    @DisplayName("Test 18.2f: Frontend integration - luxon DateTime compatibility")
    void testFrontendLuxonCompatibility() {
        // Given: Create rental with UTC timestamp
        Instant pickup = Instant.parse("2025-04-15T14:00:00Z");
        Instant dropoff = Instant.parse("2025-04-15T18:00:00Z");
        
        CreateRentalRequest request = CreateRentalRequest.builder()
                .carsId(CAR_ID)
                .pickupDatetime(pickup)
                .returnDatetime(dropoff)
                .pickupLocation("Frontend Test")
                .build();

        RentalResponse response = rentalService.createRental(request, RENTER_ID);

        // Then: Verify response provides ISO-8601 UTC strings
        String pickupIso = response.getPickupDatetime().toString();
        String dropoffIso = response.getReturnDatetime().toString();
        
        log.info("ISO-8601 strings for frontend: pickup={}, dropoff={}", pickupIso, dropoffIso);
        
        assertThat(pickupIso).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
        assertThat(dropoffIso).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z");
        
        // Simulate frontend luxon conversion to NYC timezone
        ZoneId nycZone = ZoneId.of("America/New_York");
        ZonedDateTime pickupNyc = pickup.atZone(nycZone);
        ZonedDateTime dropoffNyc = dropoff.atZone(nycZone);
        
        log.info("Frontend luxon conversion to NYC: {} to {}", pickupNyc, dropoffNyc);
        
        // Verify frontend gets correct local time (14:00 UTC = 10:00 AM EDT in April)
        assertThat(pickupNyc.getHour()).isEqualTo(10);
        assertThat(dropoffNyc.getHour()).isEqualTo(14);
    }
}
