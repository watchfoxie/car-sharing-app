package com.usarbcs.driver.mapper;


import com.usarbcs.driver.dto.NotificationDriverDto;
import com.usarbcs.driver.model.NotificationDriver;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class NotificationDriverMapper {
    public abstract NotificationDriverDto toDto(NotificationDriver notificationDriver);
}