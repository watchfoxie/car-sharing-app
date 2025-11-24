package com.usarbcs.driver.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;



@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DriverStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID", columnDefinition = "uuid")
    @EqualsAndHashCode.Include
    protected UUID id;

    private String status;

    public static DriverStatus create(String name){
        final DriverStatus driverStatus = new DriverStatus();

        driverStatus.status = name;

        return driverStatus;
    }
}
