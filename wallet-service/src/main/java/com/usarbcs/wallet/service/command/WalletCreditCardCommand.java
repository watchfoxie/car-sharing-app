package com.usarbcs.wallet.service.command;

import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertRegex;

@Getter
@Setter
public class WalletCreditCardCommand {
    private static final String CARD_NUMBER_REGEX = "^[0-9]{12,19}$";
    private static final String EXPIRATION_REGEX = "^(0[1-9]|1[0-2])/[0-9]{2}$";
    private static final String CVV_REGEX = "^[0-9]{3,4}$";

    private String holderName;
    private String alias;
    private String number;
    private String expirationDate;
    private String cvv;

    public void validate() {
        assertNotBlank(holderName);
        assertNotBlank(number);
        assertNotBlank(expirationDate);
        assertNotBlank(cvv);
        assertRegex(number, CARD_NUMBER_REGEX);
        assertRegex(expirationDate, EXPIRATION_REGEX);
        assertRegex(cvv, CVV_REGEX);
    }
}
