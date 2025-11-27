package com.usarbcs.authen.service.service;


import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.enums.RoleType;
import com.usarbcs.authen.service.exception.RoleAssignmentFailedException;
import com.usarbcs.authen.service.model.Role;
import com.usarbcs.authen.service.model.User;
import com.usarbcs.authen.service.repository.AuthRepository;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.util.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
        log.info("Begin validation for user email {}", user.getEmail());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        final Set<RoleType> requestedRoles = extractRoleTypes(user);
        user.setRoles(Collections.emptySet());

        return authRepository.findActiveByEmail(user.getEmail())
            .flatMap(existing -> Mono.<User>error(emailAlreadyExists(user.getEmail())))
                .switchIfEmpty(Mono.defer(() -> persistUser(user, requestedRoles)))
                .doOnSuccess(saved -> log.info("User {} persisted with {} role(s)", saved.getId(), saved.getRoles().size()))
                .onErrorMap(DuplicateKeyException.class, ex -> emailAlreadyExists(user.getEmail()));
    }

    private Mono<User> persistUser(User user, Set<RoleType> requestedRoles) {
        return authRepository.save(user)
            .flatMap(saved -> roleManagementService.assignRoles(saved.getId(), requestedRoles)
                .then(Mono.defer(() -> roleManagementService.loadRoles(saved.getId())))
                .flatMap(roleTypes -> validateAndAttachRoles(saved, requestedRoles, roleTypes)));
    }

    private BusinessException emailAlreadyExists(String email) {
        log.warn("An account with email {} already exists", email);
        return new BusinessException(ExceptionPayloadFactory.EMAIL_ALREADY_EXIST.get(), email);
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
