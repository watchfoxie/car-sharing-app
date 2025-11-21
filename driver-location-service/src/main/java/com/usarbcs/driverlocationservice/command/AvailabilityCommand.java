package com.usarbcs.driverlocationservice.command;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCommand {

    @NotNull(message = "available flag must be provided")
    private Boolean available;
}
