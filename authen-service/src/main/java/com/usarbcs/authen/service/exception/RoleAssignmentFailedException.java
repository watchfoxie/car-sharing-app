package com.usarbcs.authen.service.exception;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;

import java.util.UUID;

public class RoleAssignmentFailedException extends BusinessException {

    public RoleAssignmentFailedException(UUID userId, int expectedCount, int persistedCount) {
        super(ExceptionPayloadFactory.ROLE_ASSIGNMENT_FAILED.get(), userId, expectedCount, persistedCount);
    }
}
