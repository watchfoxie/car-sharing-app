package com.usarbcs.wallet.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class WalletDto {
    private UUID id;
    private UUID accountId;
    private BigDecimal balance;
    private Boolean active;
    private Boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WalletCreditCardDto> creditCards;
    private List<WalletPaymentDto> payments;
}
