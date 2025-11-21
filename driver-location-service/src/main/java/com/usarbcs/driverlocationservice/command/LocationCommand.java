package com.usarbcs.driverlocationservice.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationCommand {

    private String id;

    private Boolean active;

    private Boolean preferred;

    @Valid
    @NotNull(message = "geoIp is required")
    private GeoIpCommand geoIp;
}
