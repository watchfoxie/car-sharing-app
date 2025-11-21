package com.usarbcs.driverlocationservice.payload;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DriverLocationPayload {
    private String driverId;
    private Boolean available;
    private String carId;
    private LocalDateTime lastUpdatedAt;
    private LocationPayload lastKnownLocation;
}
