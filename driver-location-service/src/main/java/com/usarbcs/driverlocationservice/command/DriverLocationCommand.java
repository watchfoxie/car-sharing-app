package com.usarbcs.driverlocationservice.command;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationCommand {

    @NotBlank(message = "driverId must not be blank")
    private String driverId;

    @Size(max = 255, message = "name must be less than 255 characters")
    private String name;

    private Boolean available;

    @Size(max = 64, message = "carId must be less than 64 characters")
    private String carId;

    @Builder.Default
    @Valid
    private List<LocationCommand> locations = new ArrayList<>();
}
