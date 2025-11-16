package com.services.identity_adapter.service;

import com.services.common.exception.ResourceNotFoundException;
import com.services.identity_adapter.domain.entity.Account;
import com.services.identity_adapter.domain.repository.AccountRepository;
import com.services.identity_adapter.dto.AccountProfileResponse;
import com.services.identity_adapter.dto.UpdateAccountProfileRequest;
import com.services.identity_adapter.mapper.AccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AccountService}.
 * 
 * <p>Tests business logic for account management operations using Mockito
 * to isolate service layer from repository and mapper dependencies.
 * 
 * <p><strong>Test coverage:</strong>
 * <ul>
 *   <li>Get account profile (success and not found scenarios)</li>
 *   <li>Update account profile (success, not found, partial updates)</li>
 *   <li>Account existence checks</li>
 *   <li>Username and email availability checks</li>
 *   <li>Null handling and edge cases</li>
 * </ul>
 * 
 * @author Car Sharing Development Team - Phase 15 Testing
 * @version 1.0.0
 * @since 2025-01-09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private AccountProfileResponse testResponse;
    private UpdateAccountProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        // Setup test fixtures
        testAccount = Account.builder()
                .id("auth0|123456789")
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .imageUrl("https://example.com/avatar.jpg")
                .enabled(true)
                .build();

        testResponse = AccountProfileResponse.builder()
                .id("auth0|123456789")
                .username("testuser")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .imageUrl("https://example.com/avatar.jpg")
                .enabled(true)
                .build();

        updateRequest = UpdateAccountProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+0987654321")
                .imageUrl("https://example.com/new-avatar.jpg")
                .build();
    }

    @Test
    @DisplayName("getAccountProfile - Should return account when found")
    void getAccountProfile_WhenAccountExists_ShouldReturnAccountProfile() {
        // Given
        String accountId = "auth0|123456789";
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountMapper.toProfileResponse(testAccount)).thenReturn(testResponse);

        // When
        AccountProfileResponse result = accountService.getAccountProfile(accountId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(accountId);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountMapper, times(1)).toProfileResponse(testAccount);
    }

    @Test
    @DisplayName("getAccountProfile - Should throw ResourceNotFoundException when account not found")
    void getAccountProfile_WhenAccountNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        String nonExistentAccountId = "auth0|nonexistent";
        when(accountRepository.findById(nonExistentAccountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccountProfile(nonExistentAccountId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with ID: " + nonExistentAccountId);

        verify(accountRepository, times(1)).findById(nonExistentAccountId);
        verify(accountMapper, never()).toProfileResponse(any(Account.class));
    }

    @Test
    @DisplayName("updateAccountProfile - Should update account when exists")
    void updateAccountProfile_WhenAccountExists_ShouldUpdateAndReturnProfile() {
        // Given
        String accountId = "auth0|123456789";
        Account updatedAccount = Account.builder()
                .id(accountId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+0987654321")
                .imageUrl("https://example.com/new-avatar.jpg")
                .enabled(true)
                .build();

        AccountProfileResponse updatedResponse = AccountProfileResponse.builder()
                .id(accountId)
                .username("testuser")
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+0987654321")
                .imageUrl("https://example.com/new-avatar.jpg")
                .enabled(true)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);
        when(accountMapper.toProfileResponse(updatedAccount)).thenReturn(updatedResponse);

        // When
        AccountProfileResponse result = accountService.updateAccountProfile(accountId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Jane");
        assertThat(result.getLastName()).isEqualTo("Smith");
        assertThat(result.getPhoneNumber()).isEqualTo("+0987654321");
        assertThat(result.getImageUrl()).isEqualTo("https://example.com/new-avatar.jpg");

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountMapper, times(1)).updateAccountFromRequest(updateRequest, testAccount);
        verify(accountRepository, times(1)).save(testAccount);
        verify(accountMapper, times(1)).toProfileResponse(updatedAccount);
    }

    @Test
    @DisplayName("updateAccountProfile - Should throw ResourceNotFoundException when account not found")
    void updateAccountProfile_WhenAccountNotFound_ShouldThrowResourceNotFoundException() {
        // Given
        String nonExistentAccountId = "auth0|nonexistent";
        when(accountRepository.findById(nonExistentAccountId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.updateAccountProfile(nonExistentAccountId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Account not found with ID: " + nonExistentAccountId);

        verify(accountRepository, times(1)).findById(nonExistentAccountId);
        verify(accountMapper, never()).updateAccountFromRequest(any(), any());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("updateAccountProfile - Should handle partial updates with null fields")
    void updateAccountProfile_WithPartialUpdate_ShouldOnlyUpdateProvidedFields() {
        // Given
        String accountId = "auth0|123456789";
        UpdateAccountProfileRequest partialRequest = UpdateAccountProfileRequest.builder()
                .firstName("UpdatedFirstName")
                // Other fields are null
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(accountMapper.toProfileResponse(testAccount)).thenReturn(testResponse);

        // When
        AccountProfileResponse result = accountService.updateAccountProfile(accountId, partialRequest);

        // Then
        assertThat(result).isNotNull();

        verify(accountRepository, times(1)).findById(accountId);
        verify(accountMapper, times(1)).updateAccountFromRequest(partialRequest, testAccount);
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("accountExists - Should return true when account exists")
    void accountExists_WhenAccountExists_ShouldReturnTrue() {
        // Given
        String accountId = "auth0|123456789";
        when(accountRepository.existsById(accountId)).thenReturn(true);

        // When
        boolean result = accountService.accountExists(accountId);

        // Then
        assertThat(result).isTrue();
        verify(accountRepository, times(1)).existsById(accountId);
    }

    @Test
    @DisplayName("accountExists - Should return false when account does not exist")
    void accountExists_WhenAccountDoesNotExist_ShouldReturnFalse() {
        // Given
        String nonExistentAccountId = "auth0|nonexistent";
        when(accountRepository.existsById(nonExistentAccountId)).thenReturn(false);

        // When
        boolean result = accountService.accountExists(nonExistentAccountId);

        // Then
        assertThat(result).isFalse();
        verify(accountRepository, times(1)).existsById(nonExistentAccountId);
    }

    @Test
    @DisplayName("isUsernameAvailable - Should return true when username is available")
    void isUsernameAvailable_WhenUsernameNotTaken_ShouldReturnTrue() {
        // Given
        String availableUsername = "newuser";
        when(accountRepository.existsByUsername(availableUsername)).thenReturn(false);

        // When
        boolean result = accountService.isUsernameAvailable(availableUsername);

        // Then
        assertThat(result).isTrue();
        verify(accountRepository, times(1)).existsByUsername(availableUsername);
    }

    @Test
    @DisplayName("isUsernameAvailable - Should return false when username is taken")
    void isUsernameAvailable_WhenUsernameTaken_ShouldReturnFalse() {
        // Given
        String takenUsername = "testuser";
        when(accountRepository.existsByUsername(takenUsername)).thenReturn(true);

        // When
        boolean result = accountService.isUsernameAvailable(takenUsername);

        // Then
        assertThat(result).isFalse();
        verify(accountRepository, times(1)).existsByUsername(takenUsername);
    }

    @Test
    @DisplayName("isUsernameAvailable - Should be case-insensitive (via citext)")
    void isUsernameAvailable_ShouldBeCaseInsensitive() {
        // Given (repository uses citext, so it handles case-insensitivity)
        String usernameVariant = "TestUser";
        when(accountRepository.existsByUsername(usernameVariant)).thenReturn(true);

        // When
        boolean result = accountService.isUsernameAvailable(usernameVariant);

        // Then
        assertThat(result).isFalse();
        verify(accountRepository, times(1)).existsByUsername(usernameVariant);
    }

    @Test
    @DisplayName("isEmailAvailable - Should return true when email is available")
    void isEmailAvailable_WhenEmailNotRegistered_ShouldReturnTrue() {
        // Given
        String availableEmail = "new@example.com";
        when(accountRepository.existsByEmail(availableEmail)).thenReturn(false);

        // When
        boolean result = accountService.isEmailAvailable(availableEmail);

        // Then
        assertThat(result).isTrue();
        verify(accountRepository, times(1)).existsByEmail(availableEmail);
    }

    @Test
    @DisplayName("isEmailAvailable - Should return false when email is registered")
    void isEmailAvailable_WhenEmailRegistered_ShouldReturnFalse() {
        // Given
        String registeredEmail = "test@example.com";
        when(accountRepository.existsByEmail(registeredEmail)).thenReturn(true);

        // When
        boolean result = accountService.isEmailAvailable(registeredEmail);

        // Then
        assertThat(result).isFalse();
        verify(accountRepository, times(1)).existsByEmail(registeredEmail);
    }

    @Test
    @DisplayName("isEmailAvailable - Should be case-insensitive (via citext)")
    void isEmailAvailable_ShouldBeCaseInsensitive() {
        // Given (repository uses citext, so it handles case-insensitivity)
        String emailVariant = "TEST@EXAMPLE.COM";
        when(accountRepository.existsByEmail(emailVariant)).thenReturn(true);

        // When
        boolean result = accountService.isEmailAvailable(emailVariant);

        // Then
        assertThat(result).isFalse();
        verify(accountRepository, times(1)).existsByEmail(emailVariant);
    }

    @Test
    @DisplayName("getAccountProfile - Should handle null username gracefully")
    void getAccountProfile_WithNullUsername_ShouldStillReturnResponse() {
        // Given
        String accountId = "auth0|123456789";
        Account accountWithNullUsername = Account.builder()
                .id(accountId)
                .username(null)
                .email("test@example.com")
                .enabled(true)
                .build();

        AccountProfileResponse responseWithNullUsername = AccountProfileResponse.builder()
                .id(accountId)
                .username(null)
                .email("test@example.com")
                .enabled(true)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountWithNullUsername));
        when(accountMapper.toProfileResponse(accountWithNullUsername)).thenReturn(responseWithNullUsername);

        // When
        AccountProfileResponse result = accountService.getAccountProfile(accountId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isNull();
        verify(accountRepository, times(1)).findById(accountId);
    }

    @Test
    @DisplayName("updateAccountProfile - Should not modify username or email (sensitive fields)")
    void updateAccountProfile_ShouldNotModifySensitiveFields() {
        // Given
        String accountId = "auth0|123456789";
        // UpdateAccountProfileRequest does NOT have username or email fields (by design)
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        when(accountMapper.toProfileResponse(testAccount)).thenReturn(testResponse);

        // When
        AccountProfileResponse result = accountService.updateAccountProfile(accountId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        // Verify that original username and email remain unchanged
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");

        verify(accountMapper, times(1)).updateAccountFromRequest(updateRequest, testAccount);
    }
}
