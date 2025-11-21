package com.usarbcs.payment.service.mapper;

import com.usarbcs.payment.service.dto.PaymentRecordDto;
import com.usarbcs.payment.service.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(target = "paymentStatus", expression = "java(payment.getStatus().name())")
    @Mapping(target = "paymentType", expression = "java(payment.getPaymentType().name())")
    PaymentRecordDto toDto(Payment payment);

    default com.usarbcs.core.details.PaymentDto toCore(Payment payment, com.usarbcs.core.details.CreditCardDto creditCardDto) {
        if (payment == null) {
            return null;
        }
        com.usarbcs.core.details.PaymentDto dto = new com.usarbcs.core.details.PaymentDto();
        dto.setId(payment.getId() != null ? payment.getId().toString() : null);
        dto.setAmount(payment.getAmount());
        dto.setBarCode(payment.getBarCode());
        dto.setPaymentStatus(payment.getStatus().name());
        dto.setPaymentType(payment.getPaymentType().name());
        dto.setCreditCard(creditCardDto);
        return dto;
    }
}
