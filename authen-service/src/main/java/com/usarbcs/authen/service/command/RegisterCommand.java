package com.usarbcs.authen.service.command;



import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class RegisterCommand {

    @Schema(description = "User first name", example = "Maria")
    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Schema(description = "User last name", example = "Popescu")
    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Schema(description = "International phone number", example = "+37360111222", nullable = true)
    @Size(max = 25)
    @Pattern(regexp = "^(?:\\+)?\\d{7,25}$", message = "phoneNumber must contain 7-25 digits and may start with '+'")
    private String phoneNumber;

    @Schema(description = "Unique email address", example = "maria.popescu@example.com")
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Schema(description = "Raw password to hash", example = "StrongPass!123")
    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @Schema(description = "Role assigned to the new account", allowableValues = {"CLIENT", "DRIVER", "RESTAURANT", "SHOP"})
    @NotBlank
    @Pattern(regexp = "^(?i)(CLIENT|DRIVER|RESTAURANT|SHOP)$", message = "role must be CLIENT, DRIVER, RESTAURANT or SHOP")
    private String role;
}
