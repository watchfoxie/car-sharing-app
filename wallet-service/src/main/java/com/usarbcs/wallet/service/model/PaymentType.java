package com.usarbcs.wallet.service.model;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;

import java.util.Arrays;

public enum PaymentType {
    CREDIT,
    DEBIT;

    public static PaymentType fromValue(String raw) {
        if (raw == null) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(raw))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get()));
    }
}
