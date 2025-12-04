package com.usarbcs.wallet.service.command;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import static com.usarbcs.core.util.Assert.assertNotNull;

@Getter
@Setter
public class WalletCommand {
    @NotNull
    private UUID accountId;

    @PositiveOrZero
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal initialBalance;

    public void validate() {
        assertNotNull(accountId);
        if (initialBalance != null && initialBalance.signum() < 0) {
            throw new com.usarbcs.core.exception.BusinessException(
                    com.usarbcs.core.exception.ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
