package com.usarbcs.wallet.service.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;
import static com.usarbcs.core.util.Assert.assertRegex;

@Getter
@Setter
public class WalletCreditCardCommand {
    private static final String CARD_NUMBER_REGEX = "^\\d{12,19}$";
    private static final String EXPIRATION_REGEX = "^(0[1-9]|1[0-2])/\\d{2}$";
    private static final String CVV_REGEX = "^\\d{3,4}$";

    @NotBlank
    private String holderName;
    private String alias;
    @NotBlank
    @Pattern(regexp = CARD_NUMBER_REGEX)
    private String number;
    @NotBlank
    @Pattern(regexp = EXPIRATION_REGEX)
    private String expirationDate;
    @NotBlank
    @Pattern(regexp = CVV_REGEX)
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
