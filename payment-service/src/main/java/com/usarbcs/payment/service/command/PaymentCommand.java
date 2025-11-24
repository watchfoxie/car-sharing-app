package com.usarbcs.payment.service.command;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertNotNull;

@Getter
@Setter
public class PaymentCommand {

    @Schema(description = "Identifier of the bank account that will be debited or credited", example = "c0a80112-3a0d-4fc1-92a9-8e2d5a5f9c21")
    @NotNull
    private UUID bankAccountId;

    @Schema(description = "Optional credit card identifier used for card based payments", example = "b13a6d8d-1f7f-4bc8-9b1e-72dfe5b7764b")
    private UUID creditCardId;

    @Schema(description = "Positive monetary amount expressed in the account currency", example = "25.75")
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal amount;

    @Schema(description = "Movement type. Accepted values: DEBIT or CREDIT", example = "DEBIT")
    @NotBlank
    private String paymentType;

    @Schema(description = "Optional barcode supplied for bill payments", example = "83640000001012380074100512345678901234567890")
    private String barCode;

    public void validate() {
        assertNotNull(bankAccountId);
        assertNotNull(amount);
        assertNotBlank(paymentType);
        if (amount.signum() <= 0) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
