package com.usarbcs.wallet.service.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import static com.usarbcs.core.util.Assert.assertNotNull;

@Getter
@Setter
public class WalletCommand {
    private UUID accountId;
    private BigDecimal initialBalance;

    public void validate() {
        assertNotNull(accountId);
        if (initialBalance != null && initialBalance.signum() < 0) {
            throw new com.usarbcs.core.exception.BusinessException(
                    com.usarbcs.core.exception.ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
