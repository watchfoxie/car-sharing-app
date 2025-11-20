package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.enums.RoleType;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleManagementService {

    Mono<Void> assignRoles(UUID userId, Collection<RoleType> roleTypes);

    Mono<Void> revokeRoles(UUID userId);

    Mono<List<RoleType>> loadRoles(UUID userId);
}
