package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.enums.RoleType;
import com.usarbcs.authen.service.exception.RoleAssignmentFailedException;
import com.usarbcs.authen.service.model.Role;
import com.usarbcs.authen.service.model.User;
import com.usarbcs.authen.service.repository.AuthRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleManagementService roleManagementService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldEncodePasswordAndPersistRoles() {
        RegisterCommand command = buildRegisterCommand();
        UUID generatedId = UUID.randomUUID();
        User savedUser = User.create(command);
        savedUser.setId(generatedId);
        savedUser.setPassword("encoded-value");
        savedUser.setRoles(Collections.emptySet());

        when(passwordEncoder.encode(command.getPassword())).thenReturn("encoded-value");
        when(authRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(roleManagementService.assignRoles(eq(generatedId), anyCollection())).thenReturn(Mono.empty());
        when(roleManagementService.loadRoles(generatedId)).thenReturn(Mono.just(List.of(RoleType.DRIVER)));

        StepVerifier.create(authService.register(command))
                .assertNext(saved -> {
                    Assertions.assertThat(saved.getPassword()).isEqualTo("encoded-value");
                    Assertions.assertThat(saved.getRoles())
                            .extracting(Role::getRoleType)
                            .containsExactly(RoleType.DRIVER);
                })
                .verifyComplete();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(authRepository).save(captor.capture());
        User persisted = captor.getValue();
        Assertions.assertThat(persisted.getPassword()).isEqualTo("encoded-value");
        Assertions.assertThat(persisted.getRoles()).isEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<RoleType>> rolesCaptor = ArgumentCaptor.forClass((Class<Collection<RoleType>>) (Class<?>) Collection.class);
        verify(roleManagementService).assignRoles(eq(generatedId), rolesCaptor.capture());
        Assertions.assertThat(rolesCaptor.getValue()).containsExactly(RoleType.DRIVER);
        verify(roleManagementService).loadRoles(generatedId);
    }

    @Test
    void registerShouldPropagateRoleAssignmentFailure() {
        RegisterCommand command = buildRegisterCommand();
        UUID userId = UUID.randomUUID();
        User savedUser = User.create(command);
        savedUser.setId(userId);
        savedUser.setPassword("encoded-value");
        savedUser.setRoles(Collections.emptySet());

        when(passwordEncoder.encode(command.getPassword())).thenReturn("encoded-value");
        when(authRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(roleManagementService.assignRoles(eq(userId), anyCollection()))
            .thenReturn(Mono.error(new IllegalStateException("role failure")));

        StepVerifier.create(authService.register(command))
            .expectErrorMatches(throwable -> throwable instanceof IllegalStateException
                && throwable.getMessage().contains("role failure"))
            .verify();
    }

    @Test
    void registerShouldFailWhenRolesMissingAfterPersistence() {
        RegisterCommand command = buildRegisterCommand();
        UUID userId = UUID.randomUUID();
        User savedUser = User.create(command);
        savedUser.setId(userId);
        savedUser.setPassword("encoded-value");
        savedUser.setRoles(Collections.emptySet());

        when(passwordEncoder.encode(command.getPassword())).thenReturn("encoded-value");
        when(authRepository.save(any(User.class))).thenReturn(Mono.just(savedUser));
        when(roleManagementService.assignRoles(eq(userId), anyCollection())).thenReturn(Mono.empty());
        when(roleManagementService.loadRoles(userId)).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(authService.register(command))
                .expectError(RoleAssignmentFailedException.class)
                .verify();
    }

    private static RegisterCommand buildRegisterCommand() {
        RegisterCommand command = new RegisterCommand();
        ReflectionTestUtils.setField(command, "firstName", "Alice");
        ReflectionTestUtils.setField(command, "lastName", "Driver");
        ReflectionTestUtils.setField(command, "phoneNumber", "+40700000001");
        ReflectionTestUtils.setField(command, "email", "driver.one@example.com");
        ReflectionTestUtils.setField(command, "password", "Secret123!");
        ReflectionTestUtils.setField(command, "role", "driver");
        return command;
    }
}
