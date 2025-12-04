package com.usarbcs.driverlocationservice.payload;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocationPayload {
    private String ipAddress;
    private String country;
    private String city;
    private String latitude;
    private String longitude;
}
