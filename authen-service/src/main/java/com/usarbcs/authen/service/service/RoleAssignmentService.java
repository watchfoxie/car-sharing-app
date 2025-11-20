package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.enums.RoleType;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleAssignmentService {

    Mono<Void> assignRole(UUID userId, RoleType roleType);

    Mono<Void> assignRoles(UUID userId, Collection<RoleType> roleTypes);

    Mono<List<RoleType>> fetchRoles(UUID userId);

    Mono<Void> deleteRoles(UUID userId);
}
