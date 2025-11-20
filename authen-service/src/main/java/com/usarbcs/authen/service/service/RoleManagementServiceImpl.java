package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.enums.RoleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleManagementServiceImpl implements RoleManagementService {

    private final RoleAssignmentService roleAssignmentService;

    @Override
    public Mono<Void> assignRoles(UUID userId, Collection<RoleType> roleTypes) {
        Collection<RoleType> safeRoles = roleTypes == null ? Collections.emptySet() : roleTypes;
        if (safeRoles.isEmpty()) {
            log.debug("No roles provided for user {}. Existing roles will be cleared.", userId);
            return roleAssignmentService.deleteRoles(userId);
        }
        return roleAssignmentService.assignRoles(userId, safeRoles);
    }

    @Override
    public Mono<Void> revokeRoles(UUID userId) {
        return roleAssignmentService.deleteRoles(userId);
    }

    @Override
    public Mono<List<RoleType>> loadRoles(UUID userId) {
        return roleAssignmentService.fetchRoles(userId);
    }
}
