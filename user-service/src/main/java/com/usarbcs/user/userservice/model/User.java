package com.usarbcs.user.userservice.model;


import com.usarbcs.command.UserRegisterCommand;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;



@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table("user_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column("username")
    private String username;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("email")
    private String email;

    @Column("password")
    private String password;

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

    @Column("active")
    private Boolean active = true;

    public static User create(final UserRegisterCommand command) {
        final User user = new User();

        user.username = command.getUsername();
        user.firstName = command.getFirstName();
        user.lastName = command.getLastName();
        user.email = command.getEmail();
        user.password = command.getPassword();

        return user;
    }
}
