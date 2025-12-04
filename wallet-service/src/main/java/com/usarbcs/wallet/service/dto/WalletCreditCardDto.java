package com.usarbcs.wallet.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class WalletCreditCardDto {
    private UUID id;
    private String holderName;
    private String alias;
    private String brand;
    private String lastFour;
    private String maskedNumber;
    private String expirationDate;
    private LocalDateTime createdAt;
}
