package com.usarbcs.driverlocationservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class DriverLocationView {
    private String id;
    private String driverId;
    private String name;
    private Boolean available;
    private String carId;
    private Boolean deleted;
    private Boolean active;
    private LocalDateTime updatedAt;
    private Set<LocationView> locations;
}
