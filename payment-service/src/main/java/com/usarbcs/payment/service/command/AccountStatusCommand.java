package com.usarbcs.payment.service.command;

import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class AccountStatusCommand {
    private String status;

    public void validate() {
        assertNotBlank(status);
    }
}
