package com.usarbcs.driver.mapper;


import com.usarbcs.driver.dto.DriverStatusDto;
import com.usarbcs.driver.model.DriverStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public  abstract class DriverStatusMapper {
    public abstract DriverStatusDto toDto(DriverStatus driverStatus);
}
