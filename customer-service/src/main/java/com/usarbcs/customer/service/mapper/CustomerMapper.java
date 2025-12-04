package com.usarbcs.customer.service.mapper;


import com.usarbcs.customer.service.dto.CustomerDto;
import com.usarbcs.customer.service.model.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class CustomerMapper {
    public abstract CustomerDto toDto(Customer customer);
}