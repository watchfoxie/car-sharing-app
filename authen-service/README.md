# Authen Service

## Known Limitations

Spring Data R2DBC 3.2.x cannot persist nested aggregates via `@MappedCollection` (see [spring-data-relational#2040](https://github.com/spring-projects/spring-data-relational/issues/2040)). For this reason, the `AuthServiceImpl` orchestrates user persistence and role assignment explicitly:

1. Users are saved through `AuthRepository` without any roles attached.
2. `RoleAssignmentService` writes to `auth_role` using SQL executed by `DatabaseClient`.
3. Roles are reloaded and attached to the returned aggregate.

Any future changes to role persistence should respect this constraint until Spring Data introduces full support for nested entity persistence in R2DBC.
