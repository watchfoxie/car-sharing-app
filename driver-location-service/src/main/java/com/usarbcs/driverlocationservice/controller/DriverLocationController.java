package com.usarbcs.driverlocationservice.controller;

import com.usarbcs.core.constants.ResourcePath;
import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.driverlocationservice.command.AvailabilityCommand;
import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import com.usarbcs.driverlocationservice.dto.DriverLocationView;
import com.usarbcs.driverlocationservice.payload.DriverLocationPayload;
import com.usarbcs.driverlocationservice.service.DriverLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @Operation(summary = "Create driver location", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = DriverLocationCommand.class), examples = @ExampleObject(name = "CreateDriverLocationRequest", value = """
            {
                "driverId": "string",
                "name": "string",
                "available": true,
                "carId": "string",
                "locations": [
                    {
                        "active": true,
                        "preferred": true,
                        "geoIp": {
                            "id": "string",
                            "ipAddress": "string",
                            "country": "string",
                            "city": "string",
                            "latitude": "string",
                            "longitude": "string"
                        }
                    }
                ]
            }
            """))), responses = {
            @ApiResponse(responseCode = "200", description = "Driver location created", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DriverLocationDto.class)))
    })
    public ResponseEntity<DriverLocationDto> create(@Valid @RequestBody DriverLocationCommand command) {
        return ResponseEntity.ok(driverLocationService.createOrUpdate(command));
    }

    @PutMapping("/{driverId}")
    @Operation(summary = "Update driver location snapshot", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = DriverLocationCommand.class), examples = @ExampleObject(name = "UpdateDriverLocationRequest", value = """
            {
                "driverId": "string",
                "name": "string",
                "available": false,
                "carId": "string",
                "locations": [
                    {
                        "active": false,
                        "preferred": false,
                        "geoIp": {
                            "id": "string",
                            "ipAddress": "string",
                            "country": "string",
                            "city": "string",
                            "latitude": "string",
                            "longitude": "string"
                        }
                    }
                ]
            }
            """))), responses = {
            @ApiResponse(responseCode = "200", description = "Driver location updated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = DriverLocationDto.class)))
    })
    public ResponseEntity<DriverLocationDto> update(@PathVariable("driverId") String driverId,
            @Valid @RequestBody DriverLocationCommand command) {
        return ResponseEntity.ok(driverLocationService.update(driverId, command));
    }

    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverLocationDto> updateAvailability(@PathVariable("driverId") String driverId,
            @Valid @RequestBody AvailabilityCommand command) {
        return ResponseEntity.ok(driverLocationService.updateAvailability(driverId, command));
    }

    @GetMapping("/{driverId}")
    public ResponseEntity<DriverLocationPayload> ensureLocation(@PathVariable("driverId") String driverId) {
        return ResponseEntity.ok(driverLocationService.ensureDriverLocation(driverId));
    }

    @GetMapping(ResourcePath.DRIVER_LOCATION_DETAILS + "/{driverId}")
    public ResponseEntity<DriverLocationDto> getDetails(@PathVariable("driverId") String driverId) {
        return ResponseEntity.ok(driverLocationService.getDetails(driverId));
    }

    @GetMapping("/snapshot/{driverId}")
    public ResponseEntity<DriverLocationView> getSnapshot(@PathVariable("driverId") String driverId) {
        return ResponseEntity.ok(driverLocationService.getView(driverId));
    }

    @DeleteMapping("/{driverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("driverId") String driverId) {
        driverLocationService.deleteByDriverId(driverId);
    }
}
