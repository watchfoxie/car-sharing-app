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
public class UserLoginCommand {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @NotBlank(message = "email must not be blank")
    @Pattern(regexp = RegexExpressions.EMAIL, message = "email must be a well-formed email address")
    @Schema(example = "string")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, max = 64, message = "password must be between 8 and 64 characters long")
    @Schema(example = "string")
    private String password;

    public void validate(){
        Set<ConstraintViolation<UserLoginCommand>> violations = VALIDATOR.validate(this);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
