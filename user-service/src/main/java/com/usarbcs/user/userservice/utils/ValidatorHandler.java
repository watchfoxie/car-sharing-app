package com.usarbcs.user.userservice.utils;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ValidatorHandler {

    private final Validator validator;

    public ValidatorHandler(Validator validator) {
        this.validator = validator;
    }


    public <T> void validate(T o) {
        Set<ConstraintViolation<T>> violations = validator.validate(o);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
