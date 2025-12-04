package com.usarbcs.wallet.service.controller;

import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.wallet.service.command.WalletCommand;
import com.usarbcs.wallet.service.command.WalletCreditCardCommand;
import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.payload.WalletSnapshotPayload;
import com.usarbcs.wallet.service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
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
            @Operation(summary = "Create wallet")
            @ApiResponse(responseCode = "201", description = "Wallet created",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDto.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "409", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDto> create(@Valid @RequestBody WalletCommand command) {
        WalletDto wallet = walletService.create(command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(wallet.getId()).toUri();
        return ResponseEntity.created(uri).body(wallet);
    }

    @GetMapping("/{walletId}")
            @Operation(summary = "Get wallet by identifier")
            @ApiResponse(responseCode = "200", description = "Wallet found",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDto.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDto> findById(@PathVariable("walletId") UUID walletId) {
        return ResponseEntity.ok(walletService.findById(walletId));
    }

    @GetMapping("/account/{accountId}")
            @Operation(summary = "Get wallet by account identifier")
            @ApiResponse(responseCode = "200", description = "Wallet found",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDto.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDto> findByAccount(@PathVariable("accountId") UUID accountId) {
        return ResponseEntity.ok(walletService.findByAccountId(accountId));
    }

    @PostMapping("/{walletId}/credit-card")
            @Operation(summary = "Register a credit card in the wallet")
            @ApiResponse(responseCode = "200", description = "Credit card added",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDto.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "409", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDto> addCreditCard(@PathVariable("walletId") UUID walletId,
                                                   @Valid @RequestBody WalletCreditCardCommand command) {
        return ResponseEntity.ok(walletService.addCreditCard(walletId, command));
    }

    @PostMapping("/{walletId}" + WALLET_PAYMENT)
            @Operation(summary = "Register a wallet payment")
            @ApiResponse(responseCode = "200", description = "Payment registered",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDto.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "409", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDto> registerPayment(@PathVariable("walletId") UUID walletId,
                                                     @Valid @RequestBody WalletPaymentCommand command) {
        return ResponseEntity.ok(walletService.registerPayment(walletId, command));
    }

    @GetMapping("/{walletId}/snapshot")
            @Operation(summary = "Get wallet snapshot with recent payments")
            @ApiResponse(responseCode = "200", description = "Snapshot computed",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletSnapshotPayload.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletSnapshotPayload> snapshot(@PathVariable("walletId") UUID walletId) {
        return ResponseEntity.ok(walletService.snapshot(walletId));
    }

    @GetMapping("/payment/{accountId}")
            @Operation(summary = "Get wallet details for payment service")
            @ApiResponse(responseCode = "200", description = "Wallet details located",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WalletDetails.class)))
            @ApiResponse(responseCode = "400", ref = "ValidationProblem")
            @ApiResponse(responseCode = "404", ref = "BusinessProblem")
            @ApiResponse(responseCode = "500", ref = "InternalProblem")
    public ResponseEntity<WalletDetails> walletDetailsByAccount(@PathVariable("accountId") UUID accountId) {
        return ResponseEntity.ok(walletService.getWalletDetailsByAccountId(accountId));
    }
}
