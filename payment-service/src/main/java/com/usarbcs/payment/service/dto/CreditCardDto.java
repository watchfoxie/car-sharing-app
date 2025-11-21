package com.usarbcs.payment.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreditCardDto {
    private UUID id;
    private String holderName;
    private String alias;
    private String brand;
    private String lastFour;
    private String expirationDate;
}
