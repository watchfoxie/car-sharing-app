package com.usarbcs.payment.service.command;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class CreditCardCommand {
    private String holderName;
    private String alias;
    private String number;
    private String expirationDate;
    private String brand;
    private String cvv;

    public void validate() {
        assertNotBlank(holderName);
        assertNotBlank(alias);
        assertNotBlank(number);
        assertNotBlank(expirationDate);
        assertNotBlank(brand);
        assertNotBlank(cvv);
        if (number.length() < 4) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
