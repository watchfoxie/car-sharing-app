package com.usarbcs.driverlocationservice.mapper;

import com.usarbcs.core.details.DriverLocationDto;
import com.usarbcs.core.details.GeoIp;
import com.usarbcs.core.details.LocationEntityDto;
import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import com.usarbcs.driverlocationservice.command.GeoIpCommand;
import com.usarbcs.driverlocationservice.command.LocationCommand;
import com.usarbcs.driverlocationservice.dto.DriverLocationView;
import com.usarbcs.driverlocationservice.dto.LocationView;
import com.usarbcs.driverlocationservice.model.DriverLocation;
import com.usarbcs.driverlocationservice.model.GeoPoint;
import com.usarbcs.driverlocationservice.model.LocationEntity;
import com.usarbcs.driverlocationservice.payload.DriverLocationPayload;
import com.usarbcs.driverlocationservice.payload.LocationPayload;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class DriverLocationMapper {

    public DriverLocation toEntity(DriverLocationCommand command) {
        if (command == null) {
            return null;
        }
        DriverLocation driverLocation = new DriverLocation();
        driverLocation.setDriverId(command.getDriverId());
        driverLocation.setName(command.getName());
        driverLocation.setAvailable(command.getAvailable());
        driverLocation.setCarId(command.getCarId());
        Set<LocationEntity> entities = mapLocations(command);
        driverLocation.setLocationEntities(entities);
        entities.forEach(loc -> loc.setDriverLocation(driverLocation));
        return driverLocation;
    }

    public void updateEntity(DriverLocation driverLocation, DriverLocationCommand command) {
        if (driverLocation == null || command == null) {
            return;
        }
        driverLocation.apply(command);
        if (command.getLocations() != null && !command.getLocations().isEmpty()) {
            Set<LocationEntity> entities = mapLocations(command);
            driverLocation.replaceLocations(entities);
        }
    }

    private Set<LocationEntity> mapLocations(DriverLocationCommand command) {
        if (command.getLocations() == null) {
            return Set.of();
        }
        return command.getLocations().stream()
                .map(this::toLocationEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private LocationEntity toLocationEntity(LocationCommand command) {
        if (command == null) {
            return null;
        }
        LocationEntity entity = new LocationEntity();
        entity.setId(command.getId());
        entity.setPreferred(command.getPreferred());
        entity.setActive(command.getActive());
        entity.setGeoPoint(toGeoPoint(command.getGeoIp()));
        return entity;
    }

    private GeoPoint toGeoPoint(GeoIpCommand command) {
        if (command == null) {
            return null;
        }
        GeoPoint geoPoint = new GeoPoint();
        geoPoint.setId(command.getId());
        geoPoint.setIpAddress(command.getIpAddress());
        geoPoint.setCountry(command.getCountry());
        geoPoint.setCity(command.getCity());
        geoPoint.setLatitude(command.getLatitude());
        geoPoint.setLongitude(command.getLongitude());
        return geoPoint;
    }

    public DriverLocationDto toDto(DriverLocation driverLocation) {
        if (driverLocation == null) {
            return null;
        }
        DriverLocationDto dto = new DriverLocationDto();
        dto.setId(driverLocation.getId());
        dto.setDriverId(driverLocation.getDriverId());
        dto.setName(driverLocation.getName());
        dto.setAvailable(driverLocation.getAvailable());
        dto.setCarId(driverLocation.getCarId());
        dto.setActive(driverLocation.getActive());
        dto.setDeleted(driverLocation.getDeleted());
        dto.setLocationEntities(driverLocation.getLocationEntities() == null ? Set.of() : driverLocation.getLocationEntities().stream()
            .map(this::toLocationDto)
            .collect(Collectors.toSet()));
        return dto;
    }

    private LocationEntityDto toLocationDto(LocationEntity entity) {
        if (entity == null) {
            return null;
        }
        LocationEntityDto dto = new LocationEntityDto();
        dto.setId(entity.getId());
        dto.setActive(entity.getActive());
        dto.setGeoIp(toGeoIp(entity.getGeoPoint()));
        return dto;
    }

    private GeoIp toGeoIp(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        GeoIp geoIp = new GeoIp();
        geoIp.setId(geoPoint.getId());
        geoIp.setIpAddress(geoPoint.getIpAddress());
        geoIp.setCountry(geoPoint.getCountry());
        geoIp.setCity(geoPoint.getCity());
        geoIp.setLatitude(geoPoint.getLatitude());
        geoIp.setLongitude(geoPoint.getLongitude());
        return geoIp;
    }

    public DriverLocationView toView(DriverLocation driverLocation) {
        if (driverLocation == null) {
            return null;
        }
        return DriverLocationView.builder()
                .id(driverLocation.getId())
                .driverId(driverLocation.getDriverId())
                .name(driverLocation.getName())
                .available(driverLocation.getAvailable())
                .carId(driverLocation.getCarId())
                .deleted(driverLocation.getDeleted())
                .active(driverLocation.getActive())
                .updatedAt(driverLocation.getUpdatedAt())
            .locations(driverLocation.getLocationEntities() == null ? Set.of() : driverLocation.getLocationEntities().stream()
                .map(this::toLocationView)
                .collect(Collectors.toSet()))
                .build();
    }

    private LocationView toLocationView(LocationEntity entity) {
        if (entity == null) {
            return null;
        }
        GeoPoint geoPoint = entity.getGeoPoint();
        return LocationView.builder()
                .id(entity.getId())
                .active(entity.getActive())
                .preferred(entity.getPreferred())
                .ipAddress(geoPoint != null ? geoPoint.getIpAddress() : null)
                .country(geoPoint != null ? geoPoint.getCountry() : null)
                .city(geoPoint != null ? geoPoint.getCity() : null)
                .latitude(geoPoint != null ? geoPoint.getLatitude() : null)
                .longitude(geoPoint != null ? geoPoint.getLongitude() : null)
                .build();
    }

    public DriverLocationPayload toPayload(DriverLocation driverLocation) {
        if (driverLocation == null) {
            return null;
        }
        LocationPayload lastLocation = driverLocation.latestLocation()
                .map(LocationEntity::getGeoPoint)
                .map(this::toLocationPayload)
                .orElse(null);
        return DriverLocationPayload.builder()
                .driverId(driverLocation.getDriverId())
                .available(driverLocation.getAvailable())
                .carId(driverLocation.getCarId())
                .lastUpdatedAt(driverLocation.getUpdatedAt())
                .lastKnownLocation(lastLocation)
                .build();
    }

    private LocationPayload toLocationPayload(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        return LocationPayload.builder()
                .ipAddress(geoPoint.getIpAddress())
                .country(geoPoint.getCountry())
                .city(geoPoint.getCity())
                .latitude(geoPoint.getLatitude())
                .longitude(geoPoint.getLongitude())
                .build();
    }
}
