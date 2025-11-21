package com.usarbcs.payment.service.service;

import com.usarbcs.payment.service.command.AccountStatusCommand;
import com.usarbcs.payment.service.command.BankAccountCommand;
import com.usarbcs.payment.service.command.CreditCardCommand;
import com.usarbcs.payment.service.command.PaymentCommand;
import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.dto.PaymentRecordDto;
import com.usarbcs.payment.service.payload.AccountDetailsPayload;

import java.util.List;
import java.util.UUID;

public interface PaymentAccountService {
    BankAccountDto createAccount(BankAccountCommand command);

    BankAccountDto updateStatus(UUID accountId, AccountStatusCommand command);

    BankAccountDto findOne(UUID accountId);

    CreditCardDto addCard(UUID accountId, CreditCardCommand command);

    PaymentRecordDto registerPayment(PaymentCommand command);

    AccountDetailsPayload getAccountDetails(UUID accountId);

    List<PaymentRecordDto> getPayments(UUID accountId);

    com.usarbcs.core.details.BankAccount findByUserId(String userId);
}
