package com.usarbcs.authen.service.service;


import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.enums.RoleType;
import com.usarbcs.authen.service.exception.RoleAssignmentFailedException;
import com.usarbcs.authen.service.model.Role;
import com.usarbcs.authen.service.model.User;
import com.usarbcs.authen.service.repository.AuthRepository;
import com.usarbcs.core.util.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{


    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleManagementService roleManagementService;

    @Override
    @Transactional
    public Mono<User> register(RegisterCommand request) {
        log.info("Begin creating user with payload {}", JSONUtil.toJSON(request));
        final User user = User.create(request);
        log.info("User with id {} created successfully", user.getId());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        final Set<RoleType> requestedRoles = extractRoleTypes(user);
        user.setRoles(Collections.emptySet());

        return authRepository.save(user)
            .flatMap(saved -> roleManagementService.assignRoles(saved.getId(), requestedRoles)
                .then(Mono.defer(() -> roleManagementService.loadRoles(saved.getId())))
                .flatMap(roleTypes -> validateAndAttachRoles(saved, requestedRoles, roleTypes)))
                .doOnSuccess(saved -> log.info("User {} persisted with {} role(s)", saved.getId(), saved.getRoles().size()));
    }

    private Set<RoleType> extractRoleTypes(User user) {
        return user.getRoles().stream()
                .map(Role::getRoleType)
                .collect(Collectors.toSet());
    }

    private Mono<User> validateAndAttachRoles(User user, Set<RoleType> requestedRoles, List<RoleType> persistedRoleTypes) {
        List<RoleType> safePersisted = persistedRoleTypes == null ? List.of() : persistedRoleTypes;
        Set<RoleType> safeRequested = requestedRoles == null ? Collections.emptySet() : requestedRoles;
        int expected = safeRequested.size();
        int actual = safePersisted.size();
        if (actual < expected || !safePersisted.containsAll(safeRequested)) {
            return Mono.error(new RoleAssignmentFailedException(user.getId(), expected, actual));
        }
        return Mono.just(attachRoles(user, safePersisted));
    }

    private User attachRoles(User user, java.util.List<RoleType> roleTypes) {
        user.setRoles(roleTypes.stream()
            .map(roleType -> Role.createRole(roleType.name()))
                .collect(Collectors.toSet()));
        return user;
    }
}
