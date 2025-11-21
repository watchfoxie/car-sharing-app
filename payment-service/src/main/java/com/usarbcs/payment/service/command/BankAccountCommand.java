package com.usarbcs.payment.service.command;

import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class BankAccountCommand {
    private String userId;
    private String type;
    private String currency;

    public void validate() {
        assertNotBlank(userId);
        assertNotBlank(type);
        assertNotBlank(currency);
    }
}
