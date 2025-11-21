package com.usarbcs.driverlocationservice.repository;

import com.usarbcs.driverlocationservice.model.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, String> {

    Optional<DriverLocation> findByDriverId(String driverId);

    boolean existsByDriverId(String driverId);

    void deleteByDriverId(String driverId);
}
