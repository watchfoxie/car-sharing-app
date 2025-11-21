package com.usarbcs.payment.service.model;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;

import java.util.Locale;

public enum PaymentType {
    DEBIT,
    CREDIT;

    public static PaymentType fromValue(String value) {
        if (value == null) {
            return DEBIT;
        }
        try {
            return PaymentType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
    }
}
