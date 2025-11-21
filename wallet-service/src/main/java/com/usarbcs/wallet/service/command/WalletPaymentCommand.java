package com.usarbcs.wallet.service.command;

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
    private BigDecimal amount;
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
