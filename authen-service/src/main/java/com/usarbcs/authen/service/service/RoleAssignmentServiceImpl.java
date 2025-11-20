package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleAssignmentServiceImpl implements RoleAssignmentService {

    private static final String INSERT_OR_UPDATE_SQL = """
            INSERT INTO auth_role (id, user_id, role_type, updated_at)
            VALUES (:id, :userId, CAST(:roleType AS role_type_enum), :updatedAt)
            ON CONFLICT (user_id) DO UPDATE
            SET role_type = EXCLUDED.role_type,
                updated_at = EXCLUDED.updated_at
            """;

    private static final String FETCH_SQL = """
            SELECT role_type FROM auth_role WHERE user_id = :userId
            """;

        private static final String DELETE_SQL = """
            DELETE FROM auth_role WHERE user_id = :userId
            """;

        private static final String USER_ID_BIND = "userId";

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> assignRole(UUID userId, RoleType roleType) {
        return insertOrUpdateRole(userId, roleType).then();
    }

    @Override
    public Mono<Void> assignRoles(UUID userId, Collection<RoleType> roleTypes) {
        return Flux.fromIterable(roleTypes)
                .flatMap(role -> insertOrUpdateRole(userId, role))
                .then();
    }

    @Override
    public Mono<List<RoleType>> fetchRoles(UUID userId) {
        return databaseClient.sql(FETCH_SQL)
                .bind(USER_ID_BIND, userId)
                .map((row, metadata) -> RoleType.valueOf(Objects.requireNonNull(row.get("role_type", String.class))))
                .all()
                .collectList();
    }

    @Override
    public Mono<Void> deleteRoles(UUID userId) {
        return databaseClient.sql(DELETE_SQL)
                .bind(USER_ID_BIND, userId)
                .fetch()
                .rowsUpdated()
                .then();
    }

    private Mono<Long> insertOrUpdateRole(UUID userId, RoleType roleType) {
        return databaseClient.sql(INSERT_OR_UPDATE_SQL)
                .bind("id", UUID.randomUUID())
            .bind(USER_ID_BIND, userId)
                .bind("roleType", roleType.name())
                .bind("updatedAt", OffsetDateTime.now())
                .fetch()
                .rowsUpdated();
    }
}
