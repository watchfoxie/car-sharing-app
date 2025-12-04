package com.usarbcs.wallet.service.command;

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
public class WalletPaymentCommand {
    private UUID creditCardId;
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    @Schema(allowableValues = {"CREDIT", "DEBIT"})
    private String paymentType;
    private String barCode;

    public void validate() {
        assertNotNull(amount);
        assertNotBlank(paymentType);
        if (amount.signum() <= 0) {
            throw new com.usarbcs.core.exception.BusinessException(
                    com.usarbcs.core.exception.ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
