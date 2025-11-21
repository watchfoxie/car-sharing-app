package com.usarbcs.payment.service.controller;

import com.usarbcs.payment.service.command.AccountStatusCommand;
import com.usarbcs.payment.service.command.BankAccountCommand;
import com.usarbcs.payment.service.command.CreditCardCommand;
import com.usarbcs.payment.service.command.PaymentCommand;
import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.dto.PaymentRecordDto;
import com.usarbcs.payment.service.payload.AccountDetailsPayload;
import com.usarbcs.payment.service.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.usarbcs.core.constants.ResourcePath.ACCOUNT_DETAILS;
import static com.usarbcs.core.constants.ResourcePath.PAYMENT;
import static com.usarbcs.core.constants.ResourcePath.V1;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(V1 + PAYMENT)
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin
public class PaymentController {

    private final PaymentAccountService paymentAccountService;

    @PostMapping("/accounts")
    public ResponseEntity<BankAccountDto> createAccount(@RequestBody BankAccountCommand command) {
        BankAccountDto account = paymentAccountService.createAccount(command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(uri).body(account);
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<BankAccountDto> updateStatus(@PathVariable UUID accountId,
                                                       @RequestBody AccountStatusCommand command) {
        return ResponseEntity.ok(paymentAccountService.updateStatus(accountId, command));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<BankAccountDto> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.findOne(accountId));
    }

    @PostMapping("/accounts/{accountId}/credit-cards")
    public ResponseEntity<CreditCardDto> addCard(@PathVariable UUID accountId,
                                                 @RequestBody CreditCardCommand command) {
        CreditCardDto card = paymentAccountService.addCard(accountId, command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(card.getId()).toUri();
        return ResponseEntity.created(uri).body(card);
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentRecordDto> registerPayment(@RequestBody PaymentCommand command) {
        return ResponseEntity.ok(paymentAccountService.registerPayment(command));
    }

    @GetMapping("/accounts/{accountId}/details")
    public ResponseEntity<AccountDetailsPayload> getAccountDetails(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.getAccountDetails(accountId));
    }

    @GetMapping("/accounts/{accountId}/payments")
    public ResponseEntity<List<PaymentRecordDto>> getPayments(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.getPayments(accountId));
    }

    @GetMapping(ACCOUNT_DETAILS + "/{userId}")
    public ResponseEntity<com.usarbcs.core.details.BankAccount> getAccountDetailsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(paymentAccountService.findByUserId(userId));
    }
}
