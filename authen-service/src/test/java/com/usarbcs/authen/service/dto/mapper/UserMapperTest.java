package com.usarbcs.authen.service.dto.mapper;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.dto.UserDto;
import com.usarbcs.authen.service.model.Role;
import com.usarbcs.authen.service.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();
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
    void toDtoShouldMapAllFields() {
        User user = User.create(registerCommand);
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();
        user.setId(id);
        user.setActive(true);
        user.setCreatedAt(createdAt);
        user.setCreatedBy("system");
        user.setUpdatedAt(updatedAt);
        user.setUpdatedBy("tester");
        user.setDeleted(false);
        user.setRoles(Set.of(Role.createRole("DRIVER")));

        UserDto dto = mapper.toDto(user);

        assertThat(dto.getId()).isEqualTo(id.toString());
        assertThat(dto.getFirstName()).isEqualTo("Alice");
        assertThat(dto.getLastName()).isEqualTo("Driver");
        assertThat(dto.getPhoneNumber()).isEqualTo("+40700000001");
        assertThat(dto.getEmail()).isEqualTo("driver.one@example.com");
        assertThat(dto.getPassword()).isEqualTo("Secret123!");
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(dto.getCreatedBy()).isEqualTo("system");
        assertThat(dto.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(dto.getUpdatedBy()).isEqualTo("tester");
        assertThat(dto.getDeleted()).isFalse();
    }

    @Test
    void toDtoShouldReturnNullWhenUserNull() {
        assertThat(mapper.toDto(null)).isNull();
    }
}
