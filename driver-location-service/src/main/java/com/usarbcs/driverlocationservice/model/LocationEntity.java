package com.usarbcs.driverlocationservice.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

@Entity
@Table(name = "driver_location_entity")
@Getter
@Setter
public class LocationEntity extends BaseEntity {

    @Column(name = "PREFERRED")
    private Boolean preferred = Boolean.FALSE;

    @Embedded
    private GeoPoint geoPoint;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "DRIVER_LOCATION_ID")
    private DriverLocation driverLocation;

    @PrePersist
    @PreUpdate
    private void ensureFlags() {
        preferred = Optional.ofNullable(preferred).orElse(Boolean.FALSE);
        active = Optional.ofNullable(active).orElse(Boolean.TRUE);
        deleted = Optional.ofNullable(deleted).orElse(Boolean.FALSE);
    }
}
