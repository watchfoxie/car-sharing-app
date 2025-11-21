package com.usarbcs.wallet.service.controller;

import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.wallet.service.command.WalletCommand;
import com.usarbcs.wallet.service.command.WalletCreditCardCommand;
import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.payload.WalletSnapshotPayload;
import com.usarbcs.wallet.service.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

import static com.usarbcs.core.constants.ResourcePath.V1;
import static com.usarbcs.core.constants.ResourcePath.WALLET;
import static com.usarbcs.core.constants.ResourcePath.WALLET_PAYMENT;
import static org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest;

@RestController
@RequestMapping(V1 + WALLET)
@RequiredArgsConstructor
@Slf4j
@Validated
@CrossOrigin
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletDto> create(@RequestBody WalletCommand command) {
        WalletDto wallet = walletService.create(command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(wallet.getId()).toUri();
        return ResponseEntity.created(uri).body(wallet);
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletDto> findById(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.findById(walletId));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<WalletDto> findByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(walletService.findByAccountId(accountId));
    }

    @PostMapping("/{walletId}/credit-card")
    public ResponseEntity<WalletDto> addCreditCard(@PathVariable UUID walletId,
                                                   @RequestBody WalletCreditCardCommand command) {
        return ResponseEntity.ok(walletService.addCreditCard(walletId, command));
    }

    @PostMapping("/{walletId}" + WALLET_PAYMENT)
    public ResponseEntity<WalletDto> registerPayment(@PathVariable UUID walletId,
                                                     @RequestBody WalletPaymentCommand command) {
        return ResponseEntity.ok(walletService.registerPayment(walletId, command));
    }

    @GetMapping("/{walletId}/snapshot")
    public ResponseEntity<WalletSnapshotPayload> snapshot(@PathVariable UUID walletId) {
        return ResponseEntity.ok(walletService.snapshot(walletId));
    }

    @GetMapping("/payment/{accountId}")
    public ResponseEntity<WalletDetails> walletDetailsByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(walletService.getWalletDetailsByAccountId(accountId));
    }
}
