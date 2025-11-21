package com.usarbcs.driverlocationservice.service;

import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.driverlocationservice.command.AvailabilityCommand;
import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import com.usarbcs.driverlocationservice.dto.DriverLocationView;
import com.usarbcs.driverlocationservice.payload.DriverLocationPayload;

public interface DriverLocationService {

    DriverLocationDto createOrUpdate(DriverLocationCommand command);

    DriverLocationDto update(String driverId, DriverLocationCommand command);

    DriverLocationPayload ensureDriverLocation(String driverId);

    DriverLocationDto getDetails(String driverId);

    DriverLocationView getView(String driverId);

    DriverLocationDto updateAvailability(String driverId, AvailabilityCommand command);

    void deleteByDriverId(String driverId);
}
