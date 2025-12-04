package com.usarbcs.driverlocationservice.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationCommand {

    @Schema(description = "Existing driver location identifier", example = "123e4567-e89b-12d3-a456-426614174000", format = "uuid")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "location id must be a valid UUID")
    private String id;

    private Boolean active;

    private Boolean preferred;

    @Valid
    @NotNull(message = "geoIp is required")
    private GeoIpCommand geoIp;
}
