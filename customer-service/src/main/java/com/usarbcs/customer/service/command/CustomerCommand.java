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
public class CustomerCommand {
    @NotBlank
    @Size(min = 2, max = 50)
    @Schema(description = "Customer first name", example = "John")
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    @Schema(description = "Customer last name", example = "Doe")
    private String lastName;

    @NotBlank
    @Email
    @Schema(description = "Customer email address", example = "john.doe@example.com")
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    @Schema(description = "Login password (min 8 characters)", example = "Sup3rSecret!", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    public void validate(){
        assertRegex(firstName, ALPHABETIC_MIN_2_CHARS);
        assertRegex(lastName, ALPHABETIC_MIN_2_CHARS);
        assertRegex(email, EMAIL);
    }
}
