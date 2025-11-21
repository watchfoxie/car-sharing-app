package com.usarbcs.payment.service.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertNotNull;

@Getter
@Setter
public class PaymentCommand {
    private UUID bankAccountId;
    private UUID creditCardId;
    private BigDecimal amount;
    private String paymentType;
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
