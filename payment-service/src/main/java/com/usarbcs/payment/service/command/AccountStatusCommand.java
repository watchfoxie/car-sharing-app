package com.usarbcs.payment.service.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class AccountStatusCommand {

    @Schema(description = "Target lifecycle status for the bank account", example = "SUSPENDED")
    @NotBlank
    private String status;

    public void validate() {
        assertNotBlank(status);
    }
}
