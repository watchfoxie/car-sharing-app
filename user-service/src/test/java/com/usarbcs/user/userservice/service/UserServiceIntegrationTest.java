package com.usarbcs.user.userservice.service;

import com.usarbcs.command.UserLoginCommand;
import com.usarbcs.command.UserRegisterCommand;
import com.usarbcs.user.userservice.model.User;
import com.usarbcs.user.userservice.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Duration;

@Testcontainers
@SpringBootTest
class UserServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("user_service")
            .withUsername("csadmin")
            .withPassword("csadmin123");

    @DynamicPropertySource
    static void configureDataSources(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        StepVerifier.create(userRepository.deleteAll()).verifyComplete();
    }

    @Test
    void shouldPersistUserWithEncodedPassword() {
        UserRegisterCommand command = buildRegisterCommand("john.doe@example.com", "Secret123!");

        User saved = userService.create(command).block(Duration.ofSeconds(5));

        Assertions.assertThat(saved).isNotNull();
        Assertions.assertThat(saved.getId()).isNotNull();
        Assertions.assertThat(saved.getPassword()).isNotEqualTo(command.getPassword());
        Assertions.assertThat(saved.getPassword()).startsWith("$2");

        StepVerifier.create(userRepository.findActiveByEmail(command.getEmail()))
                .assertNext(found -> {
                    Assertions.assertThat(found.getUsername()).isEqualTo(command.getUsername());
                    Assertions.assertThat(found.getEmail()).isEqualTo(command.getEmail());
                })
                .verifyComplete();
    }

    @Test
    void shouldLoginUsingCaseInsensitiveEmail() {
        UserRegisterCommand registerCommand = buildRegisterCommand("case.user@example.com", "Secret123!");
        userService.create(registerCommand).block(Duration.ofSeconds(5));

        UserLoginCommand loginCommand = buildLoginCommand("CASE.USER@EXAMPLE.COM", "Secret123!");

        StepVerifier.create(userService.login(loginCommand))
                .assertNext(user -> Assertions.assertThat(user.getEmail()).isEqualTo(registerCommand.getEmail()))
                .verifyComplete();
    }

    @Test
    void shouldFailLoginWhenPasswordDoesNotMatch() {
        UserRegisterCommand registerCommand = buildRegisterCommand("wrong.pass@example.com", "Secret123!");
        userService.create(registerCommand).block(Duration.ofSeconds(5));

        UserLoginCommand loginCommand = buildLoginCommand("wrong.pass@example.com", "DifferentPass!1");

        StepVerifier.create(userService.login(loginCommand))
                .expectErrorSatisfies(throwable -> Assertions.assertThat(throwable)
                        .isInstanceOf(RuntimeException.class)
                        .hasMessage("Invalid credentials"))
                .verify();
    }

    private static UserRegisterCommand buildRegisterCommand(String email, String password) {
        UserRegisterCommand command = new UserRegisterCommand();
        ReflectionTestUtils.setField(command, "username", email.split("@")[0]);
        ReflectionTestUtils.setField(command, "firstName", "John");
        ReflectionTestUtils.setField(command, "lastName", "Tester");
        ReflectionTestUtils.setField(command, "email", email);
        ReflectionTestUtils.setField(command, "password", password);
        return command;
    }

    private static UserLoginCommand buildLoginCommand(String email, String password) {
        UserLoginCommand command = new UserLoginCommand();
        ReflectionTestUtils.setField(command, "email", email);
        ReflectionTestUtils.setField(command, "password", password);
        return command;
    }
}
