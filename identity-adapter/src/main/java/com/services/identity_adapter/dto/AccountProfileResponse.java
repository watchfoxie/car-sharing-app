package com.services.identity_adapter.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * DTO for Account profile responses.
 * 
 * <p>Used for read-only account profile information exposed via REST API.
 * 
 * <p><strong>Excludes sensitive fields:</strong> passwords, internal IDs, audit details
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Account profile information")
public class AccountProfileResponse {

    @Schema(description = "Account ID (OIDC subject)", example = "auth0|507f1f77bcf86cd799439011", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String id;

    @Schema(description = "Unique username (case-insensitive)", example = "john.doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Size(max = 255)
    private String username;

    @Schema(description = "Email address (case-insensitive)", example = "john.doe@example.com")
    @Email
    @Size(max = 255)
    private String email;

    @Schema(description = "First name", example = "John")
    @Size(max = 100)
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @Size(max = 100)
    private String lastName;

    @Schema(description = "Phone number", example = "+40712345678")
    @Size(max = 50)
    private String phoneNumber;

    @Schema(description = "Profile image URL", example = "https://cdn.example.com/avatars/johndoe.jpg")
    private String imageUrl;

    @Schema(description = "Account enabled status", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "Account creation timestamp", example = "2025-11-05T10:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdDate;

    @Schema(description = "Last modification timestamp", example = "2025-11-05T14:45:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime lastModifiedDate;
}
