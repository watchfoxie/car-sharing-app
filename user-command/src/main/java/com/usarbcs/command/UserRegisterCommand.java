package com.usarbcs.command;

import com.usarbcs.core.util.RegexExpressions;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.Set;

@Getter
public class UserRegisterCommand {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @NotBlank(message = "username must not be blank")
    @Schema(example = "string")
    private String username;

    @NotBlank(message = "firstName must not be blank")
    @Pattern(regexp = RegexExpressions.ALPHABETIC_MIN_2_CHARS,
            message = "firstName must contain only alphabetic characters and be at least 2 characters long")
    @Schema(example = "string")
    private String firstName;

    @NotBlank(message = "lastName must not be blank")
    @Pattern(regexp = RegexExpressions.ALPHABETIC_MIN_2_CHARS,
            message = "lastName must contain only alphabetic characters and be at least 2 characters long")
    @Schema(example = "string")
    private String lastName;

    @NotBlank(message = "email must not be blank")
    @Pattern(regexp = RegexExpressions.EMAIL, message = "email must be a well-formed email address")
    @Schema(example = "string")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, max = 64, message = "password must be between 8 and 64 characters long")
    @Schema(example = "string")
    private String password;

    public void validate() {
        Set<ConstraintViolation<UserRegisterCommand>> violations = VALIDATOR.validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
