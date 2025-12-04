package com.usarbcs.authen.service.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {

    @Test
    void allArgsConstructorShouldPopulateFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
        LocalDateTime updatedAt = LocalDateTime.now();
        UserDto dto = new UserDto("1", "Alice", "Driver", "+40700000001",
                "driver.one@example.com", "Secret123!", true,
                createdAt, "system", updatedAt, "tester", false);

        assertThat(dto.getId()).isEqualTo("1");
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
    void settersShouldUpdateValues() {
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now().plusHours(1);
        UserDto dto = new UserDto();

        dto.setId("2");
        dto.setFirstName("Bob");
        dto.setLastName("Rider");
        dto.setPhoneNumber("+40700000002");
        dto.setEmail("bob.rider@example.com");
        dto.setPassword("AnotherSecret#1");
        dto.setActive(false);
        dto.setCreatedAt(createdAt);
        dto.setCreatedBy("auditor");
        dto.setUpdatedAt(updatedAt);
        dto.setUpdatedBy("auditor2");
        dto.setDeleted(true);

        assertThat(dto.getId()).isEqualTo("2");
        assertThat(dto.getFirstName()).isEqualTo("Bob");
        assertThat(dto.getLastName()).isEqualTo("Rider");
        assertThat(dto.getPhoneNumber()).isEqualTo("+40700000002");
        assertThat(dto.getEmail()).isEqualTo("bob.rider@example.com");
        assertThat(dto.getPassword()).isEqualTo("AnotherSecret#1");
        assertThat(dto.isActive()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(dto.getCreatedBy()).isEqualTo("auditor");
        assertThat(dto.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(dto.getUpdatedBy()).isEqualTo("auditor2");
        assertThat(dto.getDeleted()).isTrue();
    }
}
