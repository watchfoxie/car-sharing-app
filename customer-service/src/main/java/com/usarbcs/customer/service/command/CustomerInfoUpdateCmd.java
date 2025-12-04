package com.usarbcs.customer.service.command;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import static com.usarbcs.core.util.Assert.assertRegex;
import static com.usarbcs.core.util.RegexExpressions.ALPHABETIC_MIN_2_CHARS;
import static com.usarbcs.core.util.RegexExpressions.EMAIL;

@Getter
@Setter
public class CustomerInfoUpdateCmd {
    @NotBlank
    @Size(min = 2, max = 50)
    @Schema(description = "Updated first name", example = "Jane")
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Schema(description = "Updated last name", example = "Roe")
    private String lastName;

    @NotBlank
    @Email
    @Schema(description = "Updated email address", example = "jane.roe@example.com")
    private String email;

    public void validate(){
        assertRegex(firstName, ALPHABETIC_MIN_2_CHARS);
        assertRegex(lastName, ALPHABETIC_MIN_2_CHARS);
        assertRegex(email, EMAIL);
    }
}
