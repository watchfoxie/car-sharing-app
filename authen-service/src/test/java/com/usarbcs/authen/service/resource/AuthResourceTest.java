package com.usarbcs.authen.service.resource;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.model.User;
import com.usarbcs.authen.service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthResource authResource;

    private RegisterCommand registerCommand;

    @BeforeEach
    void setUp() {
        registerCommand = new RegisterCommand();
        ReflectionTestUtils.setField(registerCommand, "firstName", "Alice");
        ReflectionTestUtils.setField(registerCommand, "lastName", "Driver");
        ReflectionTestUtils.setField(registerCommand, "phoneNumber", "+40700000001");
        ReflectionTestUtils.setField(registerCommand, "email", "driver.one@example.com");
        ReflectionTestUtils.setField(registerCommand, "password", "Secret123!");
        ReflectionTestUtils.setField(registerCommand, "role", "driver");
    }

    @Test
    void createPersonShouldDelegateRegistrationAndReturnMessage() {
        User user = User.create(registerCommand);
        user.setId(UUID.randomUUID());
        when(authService.register(registerCommand)).thenReturn(Mono.just(user));

        StepVerifier.create(authResource.createPerson(registerCommand))
            .assertNext(message -> assertThat(message)
                .isEqualTo("User created successfully with ID: " + user))
                .verifyComplete();

        verify(authService).register(registerCommand);
    }
}
