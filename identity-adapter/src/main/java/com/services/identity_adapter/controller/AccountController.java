package com.services.identity_adapter.controller;

import com.services.identity_adapter.dto.AccountProfileResponse;
import com.services.identity_adapter.dto.UpdateAccountProfileRequest;
import com.services.identity_adapter.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for account profile management.
 * 
 * <p>Provides endpoints for authenticated users to view and update their profile information.
 * 
 * <p><strong>Security:</strong> All endpoints require valid JWT token. Users can only access
 * their own profile data (enforced via JWT sub claim).
 * 
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /v1/accounts/profile - Get current user's profile</li>
 *   <li>PUT /v1/accounts/profile - Update current user's profile (non-sensitive fields only)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account Management", description = "User account profile operations")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    /**
     * Get current user's account profile.
     * 
     * <p>Returns profile information for the authenticated user (extracted from JWT sub claim).
     * 
     * @param jwt the JWT token from SecurityContext (injected automatically)
     * @return account profile response
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get current user profile",
        description = "Retrieves profile information for the authenticated user. User ID is extracted from JWT token."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountProfileResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found for the authenticated user",
            content = @Content(mediaType = "application/problem+json")
        )
    })
    public ResponseEntity<AccountProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt) {
        
        String accountId = jwt.getSubject();
        log.info("GET /v1/accounts/profile - Fetching profile for user ID: {}", accountId);
        
        AccountProfileResponse profile = accountService.getAccountProfile(accountId);
        
        log.debug("Successfully retrieved profile for user: {}", profile.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update current user's account profile (non-sensitive fields only).
     * 
     * <p>Allows updating: firstName, lastName, phoneNumber, imageUrl.
     * Does NOT allow updating: username, email, enabled status (admin-only).
     * 
     * @param jwt the JWT token from SecurityContext
     * @param request update request with new values (null fields ignored)
     * @return updated account profile response
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Update current user profile",
        description = """
            Updates non-sensitive profile fields for the authenticated user.
            
            Updatable fields: firstName, lastName, phoneNumber, imageUrl.
            Null values in request are ignored (no update performed on those fields).
            
            Restricted fields (cannot be updated via this endpoint):
            - username (managed by OIDC provider)
            - email (requires verification via OIDC provider)
            - enabled status (admin-only operation)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Profile updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountProfileResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Account not found for the authenticated user",
            content = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation error - invalid field values",
            content = @Content(mediaType = "application/problem+json")
        )
    })
    public ResponseEntity<AccountProfileResponse> updateCurrentUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountProfileRequest request) {
        
        String accountId = jwt.getSubject();
        log.info("PUT /v1/accounts/profile - Updating profile for user ID: {} with request: {}", accountId, request);
        
        AccountProfileResponse updatedProfile = accountService.updateAccountProfile(accountId, request);
        
        log.info("Successfully updated profile for user: {}", updatedProfile.getUsername());
        return ResponseEntity.ok(updatedProfile);
    }
}
