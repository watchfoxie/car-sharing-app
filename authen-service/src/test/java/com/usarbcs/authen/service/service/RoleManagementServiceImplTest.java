package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagementServiceImplTest {

    @Mock
    private RoleAssignmentService roleAssignmentService;

    private RoleManagementService roleManagementService;

    @BeforeEach
    void setUp() {
        roleManagementService = new RoleManagementServiceImpl(roleAssignmentService);
    }

    @Test
    void assignRolesShouldDelegateToAssignmentService() {
        UUID userId = UUID.randomUUID();
        when(roleAssignmentService.assignRoles(userId, List.of(RoleType.DRIVER))).thenReturn(Mono.empty());

        StepVerifier.create(roleManagementService.assignRoles(userId, List.of(RoleType.DRIVER)))
                .verifyComplete();

        verify(roleAssignmentService).assignRoles(userId, List.of(RoleType.DRIVER));
        verify(roleAssignmentService, never()).deleteRoles(any());
    }

    @Test
    void assignRolesShouldClearRolesWhenCollectionEmpty() {
        UUID userId = UUID.randomUUID();
        when(roleAssignmentService.deleteRoles(userId)).thenReturn(Mono.empty());

        StepVerifier.create(roleManagementService.assignRoles(userId, Collections.emptySet()))
                .verifyComplete();

        verify(roleAssignmentService).deleteRoles(userId);
        verify(roleAssignmentService, never()).assignRoles(any(), any());
    }
}
