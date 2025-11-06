package com.services.identity_adapter.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO for updating non-sensitive account attributes.
 * 
 * <p>Allows users to update their profile information (firstName, lastName, phoneNumber, imageUrl).
 * 
 * <p><strong>Restrictions:</strong>
 * <ul>
 *   <li>Cannot update username (managed by OIDC provider)</li>
 *   <li>Cannot update email directly (requires verification via OIDC provider)</li>
 *   <li>Cannot update enabled status (admin-only operation)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update account profile (non-sensitive attributes only)")
public class UpdateAccountProfileRequest {

    @Schema(description = "First name", example = "John")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Schema(description = "Phone number", example = "+40712345678")
    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    private String phoneNumber;

    @Schema(description = "Profile image URL", example = "https://cdn.example.com/avatars/johndoe.jpg")
    private String imageUrl;
}
