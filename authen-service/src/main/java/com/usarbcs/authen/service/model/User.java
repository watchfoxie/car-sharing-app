package com.usarbcs.authen.service.model;
import com.usarbcs.authen.service.command.RegisterCommand;
import lombok.*;
import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("auth_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {


    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("phone_number")
    private String phoneNumber;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

    @Column("active")
    private boolean active = true;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column("updated_by")
    private String updatedBy;

    @Column("deleted")
    private Boolean deleted = false;

    // Spring Data R2DBC (3.2.x) does not support persisting nested aggregates annotated with
    // @MappedCollection (see https://github.com/spring-projects/spring-data-relational/issues/2040),
    // therefore roles are managed manually via RoleManagementService instead of through the aggregate.
    @Transient
    private Set<Role> roles = new HashSet<>();


    public static User create(final RegisterCommand registerCommand){
        final User user = new User();

        user.firstName = registerCommand.getFirstName();
        user.lastName = registerCommand.getLastName();
        user.phoneNumber = registerCommand.getPhoneNumber();
        user.email = registerCommand.getEmail();
        user.password = registerCommand.getPassword();
        user.roles.add(Role.createRole(registerCommand.getRole()));
        return user;
    }
}
