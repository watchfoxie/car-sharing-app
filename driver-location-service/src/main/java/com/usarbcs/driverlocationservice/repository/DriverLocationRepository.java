package com.usarbcs.driverlocationservice.repository;

import com.usarbcs.driverlocationservice.model.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {

    Optional<DriverLocation> findByDriverId(String driverId);

    boolean existsByDriverId(String driverId);

    void deleteByDriverId(String driverId);
}
