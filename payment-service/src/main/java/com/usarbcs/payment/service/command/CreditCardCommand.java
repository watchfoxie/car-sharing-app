package com.usarbcs.payment.service.command;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertNotBlank;

@Getter
@Setter
public class CreditCardCommand {

    @Schema(description = "Full card holder name", example = "Jane Doe")
    @NotBlank
    private String holderName;

    @Schema(description = "Alias used to display the card in the UI", example = "Personal Visa")
    @NotBlank
    private String alias;

    @Schema(description = "Primary account number. Only the last four digits are persisted.", example = "4111111111111111")
    @NotBlank
    @Size(min = 4)
    private String number;

    @Schema(description = "Expiration date formatted as MM/YY", example = "07/28")
    @NotBlank
    private String expirationDate;

    @Schema(description = "Card brand name", example = "VISA")
    @NotBlank
    private String brand;

    @Schema(description = "Card security code", example = "123")
    @NotBlank
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
