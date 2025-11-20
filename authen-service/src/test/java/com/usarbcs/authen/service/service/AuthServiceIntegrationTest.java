package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.enums.RoleType;
import com.usarbcs.authen.service.repository.AuthRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class AuthServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_service")
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
    private DatabaseClient databaseClient;

    @Autowired
    private AuthRepository authRepository;

    @Autowired
    private AuthService authService;

    @BeforeEach
    void cleanDatabase() {
        databaseClient.sql("DELETE FROM auth_role").fetch().rowsUpdated().block(Duration.ofSeconds(5));
        databaseClient.sql("DELETE FROM auth_user").fetch().rowsUpdated().block(Duration.ofSeconds(5));
    }

    @Test
    void shouldRegisterUserAndAssignRole() {
        RegisterCommand registerCommand = buildRegisterCommand();

        com.usarbcs.authen.service.model.User persistedUser = authService.register(registerCommand)
                .block(Duration.ofSeconds(5));

        Assertions.assertThat(persistedUser).isNotNull();
        Assertions.assertThat(persistedUser.getEmail()).isEqualTo(registerCommand.getEmail());
        Assertions.assertThat(persistedUser.getPassword()).isNotEqualTo(registerCommand.getPassword());
        Assertions.assertThat(fetchRoleTypes(persistedUser.getId()))
                .containsExactly(RoleType.DRIVER);

        com.usarbcs.authen.service.model.User loadedUser = authRepository.findActiveByEmail(registerCommand.getEmail())
                .block(Duration.ofSeconds(5));
        Assertions.assertThat(loadedUser).isNotNull();
        Assertions.assertThat(loadedUser.getEmail()).isEqualTo(registerCommand.getEmail());
    }

    private RegisterCommand buildRegisterCommand() {
        RegisterCommand command = new RegisterCommand();
        ReflectionTestUtils.setField(command, "firstName", "Alice");
        ReflectionTestUtils.setField(command, "lastName", "Driver");
        ReflectionTestUtils.setField(command, "phoneNumber", "+40700000001");
        ReflectionTestUtils.setField(command, "email", "driver.one@example.com");
        ReflectionTestUtils.setField(command, "password", "Secret123!");
        ReflectionTestUtils.setField(command, "role", "driver");
        return command;
    }

    private List<RoleType> fetchRoleTypes(UUID userId) {
        return databaseClient.sql("SELECT role_type FROM auth_role WHERE user_id = :userId")
                .bind("userId", userId)
                .map((row, metadata) -> RoleType.valueOf(Objects.requireNonNull(row.get("role_type", String.class))))
                .all()
                .collectList()
                .block(Duration.ofSeconds(5));
    }
}
