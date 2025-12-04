package com.usarbcs.wallet.service.dto;

import com.usarbcs.wallet.service.model.PaymentStatus;
import com.usarbcs.wallet.service.model.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class WalletPaymentDto {
    private UUID id;
    private BigDecimal amount;
    private PaymentStatus status;
    private PaymentType paymentType;
    private String barCode;
    private UUID creditCardId;
    private LocalDateTime createdAt;
}
