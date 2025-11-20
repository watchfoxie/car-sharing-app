package com.usarbcs.customer.service.mapper;



import com.usarbcs.customer.service.dto.NotificationCustomerDto;
import com.usarbcs.customer.service.model.NotificationCustomer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class NotificationCustomerMapper {

    public abstract NotificationCustomerDto toDto(NotificationCustomer notificationCustomer);
}
