package com.usarbcs.driverlocationservice.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoIpCommand {

    private String id;

    @NotBlank(message = "ipAddress is mandatory")
    @Size(max = 64)
    private String ipAddress;

    @Size(max = 255)
    private String country;

    @Size(max = 255)
    private String city;

    private String latitude;
    private String longitude;
}
