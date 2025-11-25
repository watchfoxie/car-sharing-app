package com.usarbcs.payment.service.controller;

import com.usarbcs.core.details.BankAccount;
import com.usarbcs.payment.service.command.AccountStatusCommand;
import com.usarbcs.payment.service.command.BankAccountCommand;
import com.usarbcs.payment.service.command.CreditCardCommand;
import com.usarbcs.payment.service.command.PaymentCommand;
import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.dto.PaymentRecordDto;
import com.usarbcs.payment.service.payload.AccountDetailsPayload;
import com.usarbcs.payment.service.service.PaymentAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Payment account and transaction REST APIs")
public class PaymentController {

    private final PaymentAccountService paymentAccountService;

        @PostMapping("/accounts")
        @Operation(summary = "Create payment account", description = "Registers a new payment account for the authenticated user.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccountDto.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
        public ResponseEntity<BankAccountDto> createAccount(@Valid @RequestBody BankAccountCommand command) {
        BankAccountDto account = paymentAccountService.createAccount(command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(account.getId()).toUri();
        return ResponseEntity.created(uri).body(account);
    }

        @PatchMapping("/accounts/{accountId}/status")
        @Operation(summary = "Update account status", description = "Activates or deactivates an existing payment account.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccountDto.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<BankAccountDto> updateStatus(@PathVariable UUID accountId,
                                                       @Valid @RequestBody AccountStatusCommand command) {
        return ResponseEntity.ok(paymentAccountService.updateStatus(accountId, command));
    }

        @GetMapping("/accounts/{accountId}")
        @Operation(summary = "Get account", description = "Returns a single account by identifier.")
            @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Account retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccountDto.class))),
                @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
            })
    public ResponseEntity<BankAccountDto> getAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.findOne(accountId));
    }

        @PostMapping("/accounts/{accountId}/credit-cards")
        @Operation(summary = "Attach credit card", description = "Adds a credit card to the existing account.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card attached",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = CreditCardDto.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<CreditCardDto> addCard(@PathVariable UUID accountId,
                                                 @Valid @RequestBody CreditCardCommand command) {
        CreditCardDto card = paymentAccountService.addCard(accountId, command);
        URI uri = fromCurrentRequest().path("/{id}").buildAndExpand(card.getId()).toUri();
        return ResponseEntity.created(uri).body(card);
    }

        @PostMapping("/payments")
        @Operation(summary = "Register payment", description = "Creates a payment record for the supplied account and trip context.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment registered",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = PaymentRecordDto.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
    public ResponseEntity<PaymentRecordDto> registerPayment(@Valid @RequestBody PaymentCommand command) {
        return ResponseEntity.ok(paymentAccountService.registerPayment(command));
    }

        @GetMapping("/accounts/{accountId}/details")
        @Operation(summary = "Account details", description = "Returns aggregated account information including cards and balances.")
            @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Details returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountDetailsPayload.class))),
                @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
            })
    public ResponseEntity<AccountDetailsPayload> getAccountDetails(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.getAccountDetails(accountId));
    }

        @GetMapping("/accounts/{accountId}/payments")
        @Operation(summary = "List account payments", description = "Returns all payments registered for the supplied account.")
            @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Payments returned",
                    content = @Content(mediaType = "application/json",
                        array = @ArraySchema(schema = @Schema(implementation = PaymentRecordDto.class)))),
                @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
            })
    public ResponseEntity<List<PaymentRecordDto>> getPayments(@PathVariable UUID accountId) {
        return ResponseEntity.ok(paymentAccountService.getPayments(accountId));
    }

        @GetMapping(ACCOUNT_DETAILS + "/{userId}")
        @Operation(summary = "Account details by user", description = "Exposes payment account information for other microservices by user identifier.")
            @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Account returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BankAccount.class))),
                @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
            })
        public ResponseEntity<BankAccount> getAccountDetailsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(paymentAccountService.findByUserId(userId));
    }
}
