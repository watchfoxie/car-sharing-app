package com.usarbcs.driverlocationservice.model;

import com.usarbcs.driverlocationservice.command.DriverLocationCommand;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Entity
@Table(name = "driver_location")
@Getter
@Setter
public class DriverLocation extends BaseEntity {

    @Column(name = "DRIVER_ID", nullable = false, unique = true)
    private String driverId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "AVAILABLE")
    private Boolean available = Boolean.FALSE;

    @Column(name = "CAR_ID")
    private String carId;

    @OneToMany(mappedBy = "driverLocation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LocationEntity> locationEntities = new HashSet<>();

    public static DriverLocation createDefault(String driverId) {
        DriverLocation driverLocation = new DriverLocation();
        driverLocation.driverId = driverId;
        driverLocation.name = "Driver " + driverId;
        driverLocation.available = Boolean.FALSE;
        return driverLocation;
    }

    public void apply(DriverLocationCommand command) {
        if (command.getName() != null) {
            this.name = command.getName();
        }
        if (command.getAvailable() != null) {
            this.available = command.getAvailable();
        }
        if (command.getCarId() != null) {
            this.carId = command.getCarId();
        }
    }

    public void replaceLocations(Set<LocationEntity> newLocations) {
        locationEntities.clear();
        if (newLocations != null) {
            newLocations.forEach(this::addLocation);
        }
    }

    public void addLocation(LocationEntity entity) {
        if (entity == null) {
            return;
        }
        entity.setDriverLocation(this);
        locationEntities.add(entity);
    }

    public Optional<LocationEntity> latestLocation() {
        return locationEntities.stream()
                .filter(loc -> Boolean.TRUE.equals(loc.getActive()) && Boolean.FALSE.equals(loc.getDeleted()))
                .max(Comparator.comparing(LocationEntity::getUpdatedAt, Comparator.nullsLast(LocalDateTime::compareTo)));
    }

    @PrePersist
    @PreUpdate
    private void ensureFields() {
        available = Optional.ofNullable(available).orElse(Boolean.FALSE);
        active = Optional.ofNullable(active).orElse(Boolean.TRUE);
        deleted = Optional.ofNullable(deleted).orElse(Boolean.FALSE);
        locationEntities.stream().filter(Objects::nonNull).forEach(loc -> loc.setDriverLocation(this));
    }
}
