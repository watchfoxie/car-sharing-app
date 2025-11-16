package com.services.identity_adapter.testcontainers;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Singleton PostgreSQL Testcontainer shared across integration tests.
 *
 * <p>The container loads a minimal init script that prepares the identity schema
 * and enables PostgreSQL extensions (citext) so Flyway migrations can run
 * without diverging from production.
 */
@Testcontainers
public abstract class PostgresTestContainer {

    private static final String IMAGE = "postgres:16-alpine";

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(IMAGE)
            .withDatabaseName("car_sharing_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("postgres/init-extensions.sql")
            .withReuse(true);

    protected static PostgreSQLContainer<?> postgresContainer() {
        return POSTGRES;
    }
}