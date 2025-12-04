package com.usarbcs.driverlocationservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationView {
    private String id;
    private Boolean active;
    private Boolean preferred;
    private String ipAddress;
    private String country;
    private String city;
    private String latitude;
    private String longitude;
}
