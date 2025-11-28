package com.usarbcs.payment.service.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class BankAccountCommand {

    @Schema(description = "Identifier of the user that owns the bank account", example = "string")
    @NotBlank
    private String userId;

    @Schema(description = "Account type supplied by the upstream service", example = "CHECKING")
    @NotBlank
    private String type;

    @Schema(description = "Three-letter currency code", example = "string")
    @NotBlank
    private String currency;

    public void validate() {
        assertNotBlank(userId);
        assertNotBlank(type);
        assertNotBlank(currency);
    }
}
