package com.usarbcs.driver.mapper;


import com.usarbcs.driver.dto.DriverDto;
import com.usarbcs.driver.model.Driver;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = DriverStatusMapper.class)
public interface  DriverMapper {
    DriverDto toDto(Driver driver);
}