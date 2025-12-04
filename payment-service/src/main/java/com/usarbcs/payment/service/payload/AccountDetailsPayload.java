package com.usarbcs.payment.service.payload;

import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.dto.PaymentRecordDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDetailsPayload {
    private BankAccountDto account;
    private List<CreditCardDto> creditCards;
    private List<PaymentRecordDto> payments;
}
