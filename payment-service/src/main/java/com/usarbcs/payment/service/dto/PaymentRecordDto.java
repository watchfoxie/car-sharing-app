package com.usarbcs.payment.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class PaymentRecordDto {
    private UUID id;
    private BigDecimal amount;
    private String paymentStatus;
    private String paymentType;
    private String barCode;
    private LocalDateTime createdAt;
}
