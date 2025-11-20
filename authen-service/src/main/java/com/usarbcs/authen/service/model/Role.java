package com.usarbcs.authen.service.model;
import com.usarbcs.authen.service.enums.RoleType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Table("auth_role")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {
    @Id
    private UUID id;

    @Column("role_type")
    private RoleType roleType;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column("updated_by")
    private String updatedBy;

    public static Role createRole(final String roleName){
        final Role role = new Role();
        role.setRoleType(RoleType.valueOf(roleName.toUpperCase()));
        return role;
    }
}
