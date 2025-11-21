package com.usarbcs.driverlocationservice.controller;

import com.usarbcs.core.constants.ResourcePath;
import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.driverlocationservice.command.AvailabilityCommand;
import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import com.usarbcs.driverlocationservice.dto.DriverLocationView;
import com.usarbcs.driverlocationservice.payload.DriverLocationPayload;
import com.usarbcs.driverlocationservice.service.DriverLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.usarbcs.core.constants.ResourcePath.V1;

@RestController
@RequestMapping(V1 + ResourcePath.DRIVER_LOCATION)
@RequiredArgsConstructor
@Slf4j
@Validated
public class DriverLocationController {

    private final DriverLocationService driverLocationService;

    @PostMapping
    public ResponseEntity<DriverLocationDto> create(@Valid @RequestBody DriverLocationCommand command) {
        return ResponseEntity.ok(driverLocationService.createOrUpdate(command));
    }

    @PutMapping("/{driverId}")
    public ResponseEntity<DriverLocationDto> update(@PathVariable String driverId,
                                                     @Valid @RequestBody DriverLocationCommand command) {
        return ResponseEntity.ok(driverLocationService.update(driverId, command));
    }

    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverLocationDto> updateAvailability(@PathVariable String driverId,
                                                                 @Valid @RequestBody AvailabilityCommand command) {
        return ResponseEntity.ok(driverLocationService.updateAvailability(driverId, command));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverLocationPayload> ensureLocation(@PathVariable String driverId) {
        return ResponseEntity.ok(driverLocationService.ensureDriverLocation(driverId));
    }

    @GetMapping(ResourcePath.DRIVER_LOCATION_DETAILS + "/{driverId}")
    public ResponseEntity<DriverLocationDto> getDetails(@PathVariable String driverId) {
        return ResponseEntity.ok(driverLocationService.getDetails(driverId));
    }

    @GetMapping("/snapshot/{driverId}")
    public ResponseEntity<DriverLocationView> getSnapshot(@PathVariable String driverId) {
        return ResponseEntity.ok(driverLocationService.getView(driverId));
    }

    @DeleteMapping("/{driverId}")
    public ResponseEntity<Void> delete(@PathVariable String driverId) {
        driverLocationService.deleteByDriverId(driverId);
        return ResponseEntity.noContent().build();
    }
}
