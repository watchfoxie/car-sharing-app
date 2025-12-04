package com.usarbcs.driver.criteria;


import com.usarbcs.driver.model.DriverStatus;


public record DriverCriteria(String firstName, DriverStatus status) {

    @Override
    public String firstName() {
        return firstName;
    }

    @Override
    public DriverStatus status() {
        return status;
    }
}
